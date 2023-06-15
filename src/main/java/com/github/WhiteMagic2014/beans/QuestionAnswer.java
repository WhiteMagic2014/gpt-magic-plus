package com.github.WhiteMagic2014.beans;

import java.util.ArrayList;
import java.util.List;

/**
 * 依赖IndexSearcher 问答的返回对象
 */
public class QuestionAnswer {

    private String result;

    private List<String> tempResults;

    private List<String> sources;

    private int rounds;


    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public List<String> getTempResults() {
        return tempResults;
    }

    public void setTempResults(List<String> tempResults) {
        this.tempResults = tempResults;
    }

    public void addTempResult(String tempResult) {
        if (this.tempResults == null) {
            this.tempResults = new ArrayList<>();
        }
        this.tempResults.add(tempResult);
    }

    public List<String> getSources() {
        return sources;
    }

    public void setSources(List<String> sources) {
        this.sources = sources;
    }

    public void addSource(String source) {
        if (this.sources == null) {
            this.sources = new ArrayList<>();
        }
        this.sources.add(source);
    }

    public int getRounds() {
        return rounds;
    }

    public void setRounds(int rounds) {
        this.rounds = rounds;
    }

    @Override
    public String toString() {
        return "QuestionAnswer{" +
                "result='" + result + '\'' +
                ", tempResults=" + tempResults +
                ", sources=" + sources +
                ", rounds=" + rounds +
                '}';
    }
}
