package com.github.WhiteMagic2014.function;

import com.alibaba.fastjson.JSONObject;
import com.github.WhiteMagic2014.gptApi.Chat.CreateChatCompletionRequest;
import com.github.WhiteMagic2014.gptApi.Chat.pojo.ChatMessage;
import com.github.WhiteMagic2014.tool.FunctionTool;
import com.github.WhiteMagic2014.util.RequestUtil;

/**
 * @Description: 为了使用 gmp的 函数调用
 * @author: magic chen
 * @date: 2023/11/24 15:51
 **/
public abstract class GmpFunction {

    /**
     * 获得方法 名字
     *
     * @return
     */
    public abstract String getName();


    /**
     * 获得定义的方法 作为 GptTool 传输给gmp
     *
     * @return
     */
    public abstract FunctionTool getFunctionTool();

    /**
     * 方法调用实现
     *
     * @param userMessage          用户输入的 Message
     * @param assistantTempMessage gpt返回的方法调用
     * @return gpt 结合内部方法返回处理的结果
     */
    public String handleToolMessage(ChatMessage userMessage, ChatMessage assistantTempMessage) {
        JSONObject functionJson = assistantTempMessage.getTool_calls().getJSONObject(0).getJSONObject("function");
        String callId = assistantTempMessage.getTool_calls().getJSONObject(0).getString("id");
        JSONObject arguments;
        try {
            arguments = functionJson.getJSONObject("arguments");
        } catch (Exception e) {
            arguments = new JSONObject();
            arguments.put("error", "arguments转换json错误");
            arguments.put("argumentsString", functionJson.getString("arguments"));
            throw new RuntimeException("arguments转换json错误");
        }
        HandleResult handleResult = handle(arguments);
        if (handleResult.getGptProcess()) {
            CreateChatCompletionRequest request = new CreateChatCompletionRequest()
                    .addTool(getFunctionTool())
                    .model(handleResult.getGptModel())
                    .addMessage(userMessage)
                    .addMessage(assistantTempMessage)// gpt result
                    .addMessage(ChatMessage.toolMessage(callId, handleResult.getResult()));  // send a function message with function_name and custom result
            return RequestUtil.streamRequest(request);
        } else {
            return handleResult.getResult();
        }
    }

    /**
     * 方法调用实现
     *
     * @param arguments 返回方法调用的中间消息, 根据这个内容去调用内部方法
     * @return 返回内部方法的处理结果
     */
    public abstract HandleResult handle(JSONObject arguments);

}
