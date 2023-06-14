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

    // 切片的向量集合 base64数据格式
    String contextEmbeddingB64;

    // 是否使用base64格式
    Boolean base64Embedding = false;

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


    public String getContextEmbeddingB64() {
        return contextEmbeddingB64;
    }

    public void setContextEmbeddingB64(String contextEmbeddingB64) {
        this.contextEmbeddingB64 = contextEmbeddingB64;
    }

    public Boolean getBase64Embedding() {
        return base64Embedding;
    }

    public void setBase64Embedding(Boolean base64Embedding) {
        this.base64Embedding = base64Embedding;
    }

    @Override
    public String toString() {
        return "DataEmbedding{" +
                "context='" + context + '\'' +
                ", contextEmbedding=" + contextEmbedding +
                ", contextEmbeddingB64='" + contextEmbeddingB64 + '\'' +
                ", base64Embedding=" + base64Embedding +
                ", embeddingWithQuery=" + embeddingWithQuery +
                '}';
    }
    
}
