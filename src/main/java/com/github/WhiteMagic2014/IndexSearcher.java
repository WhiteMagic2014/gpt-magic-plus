package com.github.WhiteMagic2014;

import com.github.WhiteMagic2014.beans.DataIndex;

import java.util.List;

/**
 * index搜索
 */
public interface IndexSearcher {


    /**
     * 根据question 找到n个最匹配的DataIndex
     *
     * @param question
     * @return
     */
    List<DataIndex> search(String question);

}
