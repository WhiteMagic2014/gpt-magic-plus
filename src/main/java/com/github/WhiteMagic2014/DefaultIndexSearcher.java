package com.github.WhiteMagic2014;


import com.alibaba.fastjson.JSONObject;
import com.github.WhiteMagic2014.beans.DataEmbedding;
import com.github.WhiteMagic2014.beans.DataIndex;
import com.github.WhiteMagic2014.util.Distance;
import com.github.WhiteMagic2014.util.EmbeddingUtil;
import com.github.WhiteMagic2014.util.VectorUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 写的有些简单,配合gmpIndex文件搜索, 有条件的可以把数据存数据库,搭配redis,es,solar等一起使用
 */
public class DefaultIndexSearcher implements IndexSearcher {

    // 代理服务器，默认为openai官方
    private String server;

    // openai key
    private String key;


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


    public DefaultIndexSearcher(List<String> gmpIndexFilePath) {
        init(gmpIndexFilePath, null, null);
    }

    public DefaultIndexSearcher(List<String> gmpIndexFilePath, String key) {
        init(gmpIndexFilePath, null, key);
    }

    public DefaultIndexSearcher(List<String> gmpIndexFilePath, String server, String key) {
        init(gmpIndexFilePath, server, key);
    }

    private void init(List<String> gmpIndexFilePath, String server, String key) {
        this.server = server;
        this.key = key;
        allIndex = new ArrayList<>();
        for (String path : gmpIndexFilePath) {
            try (BufferedReader br = new BufferedReader(new FileReader(path))) {
                String line = "";
                while ((line = br.readLine()) != null) {
                    allIndex.add(JSONObject.parseObject(line, DataIndex.class));
                }
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }
        tagIndex = allIndex.stream()
                .flatMap(dataIndex -> dataIndex.getTags().stream().map(tag -> new AbstractMap.SimpleEntry<>(tag, dataIndex)))
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        sourceIndex = allIndex.stream().collect(Collectors.groupingBy(DataIndex::getSource));
        idIndex = allIndex.stream().collect(Collectors.toMap(DataIndex::getId, Function.identity()));
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
        if (model == 0) {
            List<Double> questionEmbedding = VectorUtil.input2Vector(server, key, question);
            // 相似度检索
            List<DataIndex> sorted = allIndex.parallelStream()
                    .peek(de -> {
                        if (de.getBase64Embedding()) {
                            de.setEmbeddingWithQuery(Distance.cosineDistance(questionEmbedding, EmbeddingUtil.embeddingB64ToDoubleList(de.getContextEmbeddingB64())));
                        } else {
                            de.setEmbeddingWithQuery(Distance.cosineDistance(questionEmbedding, de.getContextEmbedding()));
                        }
                    })
                    .sorted(Comparator.comparing(DataEmbedding::getEmbeddingWithQuery).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());
            return sorted;
        }
        return new ArrayList<>();
    }

}
