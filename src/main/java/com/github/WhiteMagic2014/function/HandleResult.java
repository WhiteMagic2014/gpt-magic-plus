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
     * 如果需要gpt再次处理，指定所使用的gpt模型
     */
    private String gptModel;

    /**
     * 内部处理结果
     */
    private String result;


    protected HandleResult(Builder builder) {
        this.gptProcess = builder.gptProcess;
        this.gptModel = builder.gptModel;
        this.result = builder.result;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getGptModel() {
        return gptModel;
    }

    public Boolean getGptProcess() {
        return gptProcess;
    }

    public String getResult() {
        return result;
    }

    @Override
    public String toString() {
        return "HandleResult{" +
                "gptProcess=" + gptProcess +
                ", gptModel='" + gptModel + '\'' +
                ", result='" + result + '\'' +
                '}';
    }


    public static class Builder {
        private Boolean gptProcess;
        private String gptModel;
        private String result;

        public Builder() {
        }

        public Builder gptProcess(Boolean gptProcess) {
            this.gptProcess = gptProcess;
            return this;
        }

        public Builder gptModel(String gptModel) {
            this.gptModel = gptModel;
            return this;
        }

        public Builder result(String result) {
            this.result = result;
            return this;
        }

        public HandleResult build() {
            if (gptProcess && (gptModel == null || gptModel.isEmpty())) {
                throw new RuntimeException("gpt再次处理,请指定模型");
            }
            return new HandleResult(this);
        }

    }

}
