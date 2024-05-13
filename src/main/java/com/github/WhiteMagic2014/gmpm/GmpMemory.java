package com.github.WhiteMagic2014.gmpm;

import com.alibaba.fastjson.JSONObject;
import com.github.WhiteMagic2014.beans.ChatLog;
import com.github.WhiteMagic2014.beans.ChatMemory;
import com.github.WhiteMagic2014.gptApi.Chat.CreateChatCompletionRequest;
import com.github.WhiteMagic2014.gptApi.Chat.pojo.ChatMessage;
import com.github.WhiteMagic2014.gptApi.GptModel;
import com.github.WhiteMagic2014.util.RequestUtil;
import com.github.WhiteMagic2014.util.VectorUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Queue;

/**
 * 自动归纳记忆的上下文对话
 */
public class GmpMemory {

    private String model = GptModel.gpt_3p5_turbo;
    private int maxTokens = 500;// 回答问题限制的 token数量
    private boolean stream = false;// 是否使用流式请求
    private float temperature = 1.0f; // 拘束 0~2 自由
    private boolean autoMem = false;// 是否自动归纳对话进行记忆 (可能会将错误的内容记忆 造成影响)

    private MemContext context;


    public GmpMemory() {
        this.context = new DefaultMemContext();
    }

    public GmpMemory(MemContext context) {
        this.context = context;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }


    public String memoryChat(String session, String prompt) {
        String personal = context.getPersonality(session);
        // 构造初始请求
        CreateChatCompletionRequest request = new CreateChatCompletionRequest()
                .model(model)
                .maxTokens(maxTokens)
                .addMessage(ChatMessage.systemMessage(personal));
        if (temperature > 0.0f) {
            request.temperature(temperature);
        }
        // 检查是否需要更多记忆
        if (check(session, prompt)) {
            List<ChatMemory> tempMemories = context.searchMemory(session, prompt);
            for (ChatMemory memory : tempMemories) {
                request.addMessage(ChatMessage.userMessage(memory.getUser()));
                request.addMessage(ChatMessage.assistantMessage(memory.getAssistant()));
            }
        }
        // 拼接历史对话记录
        Queue<ChatLog> queue = context.chatLogs(session);
        if (queue != null && !queue.isEmpty()) {
            queue.forEach(l -> {
                request.addMessage(ChatMessage.userMessage(l.getUser()));
                request.addMessage(ChatMessage.assistantMessage(l.getAssistant()));
            });
        }
        request.addMessage(ChatMessage.userMessage(prompt));
        // 发送请求
        String result = "";
        try {
            if (stream) {
                result = RequestUtil.streamRequest(request);
            } else {
                result = (String) request.sendForChoices().get(0).getMessage().getContent();
            }
        } catch (Exception e) {
            try {
                JSONObject js = JSONObject.parseObject(e.getMessage());
                String code = js.getJSONObject("error").getString("code");
                // 如果是长度超了。 遗忘一段记忆
                if (code.equals("context_length_exceeded")) {
                    if (queue != null && !queue.isEmpty()) {
                        queue.poll();
                    }
                    // 再次请求
                    return memoryChat(session, prompt);
                } else {
                    return code;
                }
            } catch (Exception ex) {
                return e.getMessage();
            }
        }
        // 上下文
        context.addChatLog(session, prompt, result);
        // 自动加入记忆
        if (autoMem) {
            addChatMemory(session, prompt, result);
        }
        return result;
    }


    private boolean check(String session, String prompt) {
        StringBuilder sb = new StringBuilder();
        sb.append("请判断以下对话是否需要更多上下文来回复，仅回答[需要,不需要]：\n对话内容：\n");
        Queue<ChatLog> queue = context.chatLogs(session);
        if (queue != null && !queue.isEmpty()) {
            queue.forEach(l -> {
                sb.append("\nuser:").append(l.getUser());
                sb.append("\nassistant:").append(l.getAssistant());
            });
        }
        sb.append("\nuser:").append(prompt);
        String checkResult = originChat(Collections.singletonList(ChatMessage.userMessage(sb.toString())));
        System.out.println("记忆联想：" + !checkResult.contains("不需要"));
        return !checkResult.contains("不需要");
    }


    /**
     * 将一轮指定的对话加入记忆
     *
     * @param session
     * @param user
     * @param assistant
     */
    public void addChatMemory(String session, String user, String assistant) {
        String topic = originChat(Collections.singletonList(ChatMessage.userMessage("请概括以下user和assistant的对话:\n" + "user:\n" + user + "\nassistant:\n" + assistant)));
        System.out.println("对话归纳：" + topic);

        ChatMemory memory = new ChatMemory();
        memory.setUser(user);
        memory.setAssistant(assistant);
        memory.setTopic(topic);
        List<Double> vectors = VectorUtil.input2Vector(topic);
        memory.setEmbedding(vectors);
        // 加入记忆
        context.addMemory(session, memory);
    }


    /**
     * 将session中最后一轮对话 归纳后加入记忆
     *
     * @param session
     */
    public void addLastMemory(String session) {
        Queue<ChatLog> queue = context.chatLogs(session);
        if (queue == null || queue.isEmpty()) {
            return;
        }
        ChatLog lastElement = null;
        for (ChatLog element : queue) {
            lastElement = element;
        }
        String user = lastElement.getUser();
        String assistant = lastElement.getAssistant();
        addChatMemory(session, user, assistant);
    }

    private String originChat(List<ChatMessage> messages) {
        CreateChatCompletionRequest request = new CreateChatCompletionRequest()
                .maxTokens(maxTokens);
        if (StringUtils.isNotBlank(model)) {
            request.model(model);
        }
        for (ChatMessage msg : messages) {
            request.addMessage(msg);
        }
        String result = "";
        try {
            if (stream) {
                result = RequestUtil.streamRequest(request);
            } else {
                result = (String) request.sendForChoices().get(0).getMessage().getContent();
            }
        } catch (Exception e) {
            return "很抱歉，出错了";
        }
        return result.trim();
    }

}
