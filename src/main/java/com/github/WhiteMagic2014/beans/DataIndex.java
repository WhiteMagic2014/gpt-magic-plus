package com.github.WhiteMagic2014.beans;


import java.io.Serializable;
import java.util.List;

/**
 * 更完善的向量索引
 */
public class DataIndex extends DataEmbedding implements Serializable {

    private static final long serialVersionUID = 1L;

    String id;

    // 标签 可以让ai打标签，也可以后期人工标注
    List<String> tags;

    // 上一个节点id
    String beforeId;

    // 下一个节点id
    String afterId;

    // 来源
    String source;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getBeforeId() {
        return beforeId;
    }

    public void setBeforeId(String beforeId) {
        this.beforeId = beforeId;
    }

    public String getAfterId() {
        return afterId;
    }

    public void setAfterId(String afterId) {
        this.afterId = afterId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return "DataIndex{" +
                "id='" + id + '\'' +
                ", tags=" + tags +
                ", beforeId='" + beforeId + '\'' +
                ", afterId='" + afterId + '\'' +
                ", source='" + source + '\'' +
                ", context='" + context + '\'' +
                ", contextEmbedding=" + contextEmbedding +
                ", embeddingWithQuery=" + embeddingWithQuery +
                '}';
    }

}
