package com.github.WhiteMagic2014.function;

/**
 * @Description: 内部代理返回的结果
 * @author: magic chen
 * @date: 2023/11/24 17:32
 **/
public class HandleResult {

    /**
     * 处理的结果是否需要gpt再次处理，正常的流程下，内部handle返回的结果需要 再次返回给gpt 合并处理的(抽象父类里写的方法)，但有些情况不一定需要，可以直接返回
     */
    private Boolean gptProcess;

    /**
     * 内部处理结果
     */
    private String result;

    public HandleResult(Boolean gptProcess, String result) {
        this.gptProcess = gptProcess;
        this.result = result;
    }

    public Boolean getGptProcess() {
        return gptProcess;
    }

    public void setGptProcess(Boolean gptProcess) {
        this.gptProcess = gptProcess;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
