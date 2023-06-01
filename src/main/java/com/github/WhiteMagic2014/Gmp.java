package com.github.WhiteMagic2014;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.WhiteMagic2014.beans.ChatLog;
import com.github.WhiteMagic2014.beans.DataEmbedding;
import com.github.WhiteMagic2014.beans.GptMessage;
import com.github.WhiteMagic2014.gptApi.Chat.CreateChatCompletionRequest;
import com.github.WhiteMagic2014.gptApi.Embeddings.CreateEmbeddingsRequest;
import com.github.WhiteMagic2014.gptApi.Images.CreateImageRequest;
import com.github.WhiteMagic2014.util.Distance;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Gmp {

    private Map<String, Queue<ChatLog>> logs = new HashMap<>(); // 对话上下文
    private Map<String, String> personality = new HashMap<>(); //性格设定
    private ExecutorService executorService = Executors.newFixedThreadPool(50);// 线程池

    private int maxLog = 5; // 最大记忆层数

    private String server; // 代理服务器，默认为openai官方

    private String key; // openai key

    private String org; // openai org

    public Gmp() {
    }

    public Gmp(String key) {
        this.key = key;
    }

    public Gmp(String server, String key) {
        this.server = server;
        this.key = key;
    }

    public void setMaxLog(int maxLog) {
        this.maxLog = maxLog;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setOrg(String org) {
        this.org = org;
    }

    public String originChat(List<GptMessage> messages, int maxTokens) {
        CreateChatCompletionRequest request = new CreateChatCompletionRequest()
                .key(key)
                .maxTokens(maxTokens);
        if (StringUtils.isNotBlank(server)) {
            request.server(server);
        }
        if (StringUtils.isNotBlank(org)) {
            request.organization(org);
        }
        for (GptMessage msg : messages) {
            request.addMessage(msg.getRole(), msg.getPrompt());
        }
        String result = "";
        try {
            result = request.sendForChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            return "很抱歉，出错了";
        }
        return result.trim();
    }

    public String originChat(List<GptMessage> messages) {
        return originChat(messages, 500);
    }


    public String originChatStream(List<GptMessage> messages, int maxTokens) {
        StringBuilder sb = new StringBuilder();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        executorService.execute(() -> {
                    CreateChatCompletionRequest request = new CreateChatCompletionRequest()
                            .key(key)
                            .maxTokens(maxTokens)
                            .stream(true)
                            .outputStream(baos);
                    if (StringUtils.isNotBlank(server)) {
                        request.server(server);
                    }
                    if (StringUtils.isNotBlank(org)) {
                        request.organization(org);
                    }
                    for (GptMessage msg : messages) {
                        request.addMessage(msg.getRole(), msg.getPrompt());
                    }
                    request.send();
                }
        );
        boolean flag = true;
        while (flag) {
            byte[] data = baos.toByteArray();
            if (data.length > 0) {
                String result = new String(data);
                baos.reset();
                String str = "[" + result.replace("data: [DONE]", "").replace("data:", ",") + "]";
                JSONArray jsonArray;
                try {
                    jsonArray = JSON.parseArray(str);
                } catch (Exception e) {
                    System.out.println(str);
                    return "很抱歉，出错了";
                }
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject choice = jsonArray.getJSONObject(i).getJSONArray("choices").getJSONObject(0);
                    if ("stop".equals(choice.getString("finish_reason"))) {
                        flag = false;
                        break;
                    }
                    JSONObject delta = choice.getJSONObject("delta");
                    if (delta.containsKey("content")) {
                        sb.append(delta.getString("content"));
                    }
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }


    public String originChatStream(List<GptMessage> messages) {
        return originChatStream(messages, 500);
    }


    public String chat(String session, String prompt, int maxTokens) {
        String personal = personality.getOrDefault(session, "与用户进行闲聊或娱乐性的对话，以改善用户体验。");
        // 构造初始请求
        CreateChatCompletionRequest request = new CreateChatCompletionRequest()
                .key(key)
                .maxTokens(maxTokens)
                .addMessage("system", personal);
        // 如果配置了代理服务器，则使用代理服务
        if (StringUtils.isNotBlank(server)) {
            request.server(server);
        }
        if (StringUtils.isNotBlank(org)) {
            request.organization(org);
        }
        // 拼接历史对话记录
        if (logs.containsKey(session)) {
            Queue<ChatLog> queue = logs.get(session);
            queue.forEach(l -> {
                request.addMessage("user", l.getUser());
                request.addMessage("assistant", l.getAssistant());
            });
        }
        request.addMessage("user", prompt);

        // 发送请求
        String result = "";
        try {
            result = request.sendForChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            try {
                JSONObject js = JSONObject.parseObject(e.getMessage());
                // 如果是长度超了。 遗忘一段记忆
                if (js.getJSONObject("error").getString("code").equals("context_length_exceeded")) {
                    if (logs.containsKey(session)) {
                        Queue<ChatLog> queue = logs.get(session);
                        queue.poll();
                        // 再次请求
                        return chat(session, prompt, maxTokens);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return "很抱歉，出错了";
        }
        // 记忆上下文
        addChatLog(session, prompt, result);
        return result;
    }


    /**
     * 记忆上下文
     */
    public void addChatLog(String session, String user, String assistant) {
        if (logs.containsKey(session)) {
            Queue<ChatLog> queue = logs.get(session);
            if (queue.size() > maxLog) {
                queue.poll();
            }
            queue.offer(new ChatLog(user, assistant));
        } else {
            Queue<ChatLog> queue = new LinkedList<>();
            queue.offer(new ChatLog(user, assistant));
            logs.put(session, queue);
        }
    }

    public void setChatLog(String session, Queue<ChatLog> chatLogs) {
        logs.put(session, chatLogs);
    }


    public String chat(String session, String prompt) {
        return chat(session, prompt, 500);
    }

    public String setPersonality(String session, String setting) {
        personality.put(session, setting);
        return "已经设定为: " + setting;
    }

    public String clearLog(String session) {
        logs.remove(session);
        return "操作成功";
    }

    public List<String> image(String prompt, int n) {
        CreateImageRequest request = new CreateImageRequest()
                .key(key)
                .prompt(prompt)
                .n(n)
                .largeSize();
        // 如果配置了代理服务器，则使用代理服务
        if (StringUtils.isNotBlank(server)) {
            request.server(server);
        }
        if (StringUtils.isNotBlank(org)) {
            request.organization(org);
        }
        JSONObject temp = null;
        try {
            temp = request.send();
        } catch (Exception e) {
            return Collections.singletonList("出错了");
        }
        List<String> resultList = new ArrayList<>();
        JSONArray array = temp.getJSONArray("data");
        for (int i = 0; i < array.size(); i++) {
            JSONObject jsonObject = array.getJSONObject(i);
            resultList.add(jsonObject.getString("url"));
        }
        return resultList;
    }


    public List<List<Double>> input2Vector(List<String> inputs) {
        CreateEmbeddingsRequest request = new CreateEmbeddingsRequest()
                .key(key);
        // 如果配置了代理服务器，则使用代理服务
        if (StringUtils.isNotBlank(server)) {
            request.server(server);
        }
        if (inputs.size() == 1) {
            request.input(inputs.get(0));
        } else {
            String[] ins = new String[inputs.size()];
            inputs.toArray(ins);
            request.inputs(ins);
        }
        return request.sendForEmbeddings();
    }


    public String answer(String question, List<DataEmbedding> dataEmbeddings, int vectorNum) {
        if (dataEmbeddings.isEmpty()) {
            return "无预训练数据";
        }
        List<Double> questionEmbedding = input2Vector(Collections.singletonList(question)).get(0);
        List<DataEmbedding> sorted = dataEmbeddings.parallelStream()
                .peek(de -> de.setEmbeddingWithQuery(Distance.cosineDistance(questionEmbedding, de.getContextEmbedding())))
                .sorted(Comparator.comparing(DataEmbedding::getEmbeddingWithQuery).reversed())
                .collect(Collectors.toList());
        List<GptMessage> messages = new ArrayList<>();
        messages.add(GptMessage.systemMessage("根据下面的参考回答问题，请直接回答问题，如果无法回答问题，回答“我不知道”。"));
        StringBuilder prompt = new StringBuilder("参考:\n");
        for (int i = 0; i < Math.min(vectorNum, sorted.size()); i++) {
            prompt.append(sorted.get(i).getContext());
        }
        prompt.append("\n问题:\n").append(question);
        messages.add(GptMessage.userMessage(prompt.toString()));
        return originChat(messages);
    }

    public String answer(String question, List<DataEmbedding> dataEmbeddings) {
        return answer(question, dataEmbeddings, 3);
    }


}