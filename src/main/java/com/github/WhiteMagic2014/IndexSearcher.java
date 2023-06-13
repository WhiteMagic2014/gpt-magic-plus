package com.github.WhiteMagic2014;

import com.github.WhiteMagic2014.beans.DataIndex;

import java.util.List;
import java.util.Set;

/**
 * index搜索
 */
public interface IndexSearcher {

    /**
     * 根据id获得 index
     *
     * @param id
     * @return
     */
    DataIndex getIndexById(String id);

    /**
     * 根据id获得 下一个index
     *
     * @param id
     * @return
     */
    DataIndex getNextIndexById(String id);

    /**
     * 根据id获得 上一个index
     *
     * @param id
     * @return
     */
    DataIndex getLastIndexById(String id);

    /**
     * 根据标签搜索 index
     *
     * @param includeTag 需要包含的tag   where tag in（includeTag）
     * @param excludeTag 不能包含的tag   where tag not in（excludeTag）
     * @return
     */
    List<DataIndex> getIndexByTag(Set<String> includeTag, Set<String> excludeTag);

    /**
     * 根据标签搜索 index
     *
     * @param tag
     * @return
     */
    List<DataIndex> getIndexByTag(String tag);

    /**
     * 根据来源搜索 index
     *
     * @param source
     * @return
     */
    List<DataIndex> getIndexBySource(String source);

}
