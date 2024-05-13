package com.github.WhiteMagic2014.beans;

import java.util.List;

public class ChatMemory {

    // 用户文本
    private String user;
    // 回复文本
    private String assistant;
    // 对话话题
    private String topic;
    // 对话话题向量
    private List<Double> embedding;
    private Double embeddingWithQuery;


    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getAssistant() {
        return assistant;
    }

    public void setAssistant(String assistant) {
        this.assistant = assistant;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public List<Double> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<Double> embedding) {
        this.embedding = embedding;
    }

    public Double getEmbeddingWithQuery() {
        return embeddingWithQuery;
    }

    public void setEmbeddingWithQuery(Double embeddingWithQuery) {
        this.embeddingWithQuery = embeddingWithQuery;
    }
}
