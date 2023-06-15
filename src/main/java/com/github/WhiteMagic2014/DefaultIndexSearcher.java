package com.github.WhiteMagic2014;


import com.alibaba.fastjson.JSONObject;
import com.github.WhiteMagic2014.beans.DataIndex;
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

    private List<DataIndex> allIndex;
    private Map<String, List<DataIndex>> tagIndex;
    private Map<String, List<DataIndex>> sourceIndex;
    private Map<String, DataIndex> idIndex;

    public DefaultIndexSearcher(List<String> gmpIndexFilePath) {
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
        return null;
    }
}
