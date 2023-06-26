package com.github.WhiteMagic2014;


import com.alibaba.fastjson.JSONObject;
import com.github.WhiteMagic2014.beans.DataEmbedding;
import com.github.WhiteMagic2014.beans.DataIndex;
import com.github.WhiteMagic2014.util.Distance;
import com.github.WhiteMagic2014.util.EmbeddingUtil;
import com.github.WhiteMagic2014.util.VectorUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 写的有些简单,配合gmpIndex文件搜索, 有条件的可以把数据存数据库,搭配redis,es,solar等一起使用
 */
public class DefaultIndexSearcher implements IndexSearcher {

    // 检索模式 0 = 按相似度检索
    private int model = 0;

    /**
     * 根据DataIndex的content 与 question的相似度检索
     *
     * @return
     */
    public DefaultIndexSearcher contentSimilarityModel() {
        model = 0;
        return this;
    }

    /**
     * 根据DataIndex的content 与 question的相似度检索 并且联系每一个DataIndex的上下文
     *
     * @return
     */
    public DefaultIndexSearcher contentSimilarityContextModel() {
        model = 1;
        return this;
    }

    // 希望获得DataIndex数量的限制 , 由于DataIndex数量可能会少，则实际情况下 n <= limit
    private int limit = 3;

    public DefaultIndexSearcher limit(int limit) {
        this.limit = limit;
        return this;
    }

    private List<DataIndex> allIndex;
    private Map<String, List<DataIndex>> tagIndex;
    private Map<String, List<DataIndex>> sourceIndex;
    private Map<String, DataIndex> idIndex;

    public List<DataIndex> getAllIndex() {
        return allIndex;
    }

    public DefaultIndexSearcher(String storagePath) {
        load(storagePath);
    }

    public DefaultIndexSearcher(List<String> gmpIndexFilePath) {
        load(gmpIndexFilePath);
    }

    public void load(String storagePath) {
        File folder = new File(storagePath);
        if (!folder.exists() || !folder.isDirectory()) {
            throw new RuntimeException(storagePath + " 文件夹不存在");
        }
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null || listOfFiles.length == 0) {
            throw new RuntimeException(storagePath + " 文件夹下没有 .gmpIndex 文件");
        }
        List<String> gmpIndexFilePath = Arrays.stream(listOfFiles)
                .filter(File::isFile)
                .map(File::getAbsolutePath)
                .filter(name -> name.endsWith(".gmpIndex"))
                .collect(Collectors.toList());
        if (gmpIndexFilePath.isEmpty()) {
            throw new RuntimeException(storagePath + " 文件夹下没有 .gmpIndex 文件");
        }
        load(gmpIndexFilePath);
    }

    public void load(List<String> gmpIndexFilePath) {
        List<DataIndex> allIndexTemp;
        Map<String, List<DataIndex>> tagIndexTemp;
        Map<String, List<DataIndex>> sourceIndexTemp;
        Map<String, DataIndex> idIndexTemp;
        try {
            allIndexTemp = new ArrayList<>();
            for (String path : gmpIndexFilePath) {
                try (BufferedReader br = new BufferedReader(new FileReader(path))) {
                    String line = "";
                    while ((line = br.readLine()) != null) {
                        allIndexTemp.add(JSONObject.parseObject(line, DataIndex.class));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
            tagIndexTemp = allIndexTemp.stream()
                    .filter(dataIndex -> dataIndex.getTags() != null && !dataIndex.getTags().isEmpty())
                    .flatMap(dataIndex -> dataIndex.getTags().stream().map(tag -> new AbstractMap.SimpleEntry<>(tag, dataIndex)))
                    .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
            sourceIndexTemp = allIndexTemp.stream().collect(Collectors.groupingBy(index -> JSONObject.parseObject(index.getSource()).getString("source")));
            idIndexTemp = allIndexTemp.stream().collect(Collectors.toMap(DataIndex::getId, Function.identity()));
        } catch (Exception e) {
            throw new RuntimeException("reload失败,", e);
        }
        allIndex = allIndexTemp;
        tagIndex = tagIndexTemp;
        sourceIndex = sourceIndexTemp;
        idIndex = idIndexTemp;
    }


    private DataIndex getIndexById(String id) {
        return idIndex.get(id);
    }

    private DataIndex getNextIndexById(String id) {
        DataIndex tmp = idIndex.get(id);
        if (StringUtils.isNotBlank(tmp.getAfterId())) {
            return idIndex.get(tmp.getAfterId());
        }
        return null;
    }

    private DataIndex getLastIndexById(String id) {
        DataIndex tmp = idIndex.get(id);
        if (StringUtils.isNotBlank(tmp.getBeforeId())) {
            return idIndex.get(tmp.getBeforeId());
        }
        return null;
    }

    private List<DataIndex> getIndexByTag(Set<String> includeTag, Set<String> excludeTag) {
        return allIndex.stream()
                .filter(index -> new HashSet<>(index.getTags()).containsAll(includeTag))
                .filter(index -> excludeTag.stream().noneMatch(index.getTags()::contains))
                .collect(Collectors.toList());
    }


    private List<DataIndex> getIndexByTag(String tag) {
        return tagIndex.get(tag);
    }

    private List<DataIndex> getIndexBySource(String source) {
        return sourceIndex.entrySet().stream()
                .filter(entry -> entry.getKey().contains(source))
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<DataIndex> search(String question) {
        List<Double> questionEmbedding = VectorUtil.input2Vector(question);
        if (model == 0) {
            // 相似度检索
            return new ArrayList<>(allIndex).parallelStream()
                    .peek(index -> {
                        if (index.getBase64Embedding()) {
                            index.setEmbeddingWithQuery(Distance.cosineDistance(questionEmbedding, EmbeddingUtil.embeddingB64ToDoubleList(index.getContextEmbeddingB64())));
                        } else {
                            index.setEmbeddingWithQuery(Distance.cosineDistance(questionEmbedding, index.getContextEmbedding()));
                        }
                    })
                    .sorted(Comparator.comparing(DataEmbedding::getEmbeddingWithQuery).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());
        } else if (model == 1) {
            // 首先相似度检索 然后获取每个 DataIndex上下文的关联
            return new ArrayList<>(allIndex).parallelStream()
                    .peek(index -> {
                        if (index.getBase64Embedding()) {
                            index.setEmbeddingWithQuery(Distance.cosineDistance(questionEmbedding, EmbeddingUtil.embeddingB64ToDoubleList(index.getContextEmbeddingB64())));
                        } else {
                            index.setEmbeddingWithQuery(Distance.cosineDistance(questionEmbedding, index.getContextEmbedding()));
                        }
                    })
                    .sorted(Comparator.comparing(DataEmbedding::getEmbeddingWithQuery).reversed())
                    .limit(limit)
                    .map(index -> {
                        List<DataIndex> tmp = new ArrayList<>();
                        tmp.add(index);
                        tmp.add(getLastIndexById(index.getId()));
                        tmp.add(getNextIndexById(index.getId()));
                        return tmp;
                    })
                    .flatMap(List::stream)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

}
