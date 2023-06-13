package com.github.WhiteMagic2014.beans;

import java.io.Serializable;
import java.util.List;

/**
 * gpt用 数据向量模型
 */
public class DataEmbedding implements Serializable {

    private static final long serialVersionUID = 1L;

    // 切片的内容
    String context;

    // 切片的向量集合
    List<Double> contextEmbedding;

    // 用来存储问题与本切片距离 临时排序用的字段
    Double embeddingWithQuery;


    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public List<Double> getContextEmbedding() {
        return contextEmbedding;
    }

    public void setContextEmbedding(List<Double> contextEmbedding) {
        this.contextEmbedding = contextEmbedding;
    }

    public Double getEmbeddingWithQuery() {
        return embeddingWithQuery;
    }

    public void setEmbeddingWithQuery(Double embeddingWithQuery) {
        this.embeddingWithQuery = embeddingWithQuery;
    }

    @Override
    public String toString() {
        return "DataEmbedding{" +
                "context='" + context + '\'' +
                ", contextEmbedding=" + contextEmbedding +
                ", embeddingWithQuery=" + embeddingWithQuery +
                '}';
    }
}
