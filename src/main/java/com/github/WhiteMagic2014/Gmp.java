package com.github.WhiteMagic2014;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.WhiteMagic2014.beans.ChatLog;
import com.github.WhiteMagic2014.beans.DataEmbedding;
import com.github.WhiteMagic2014.gptApi.Chat.CreateChatCompletionRequest;
import com.github.WhiteMagic2014.gptApi.Chat.pojo.ChatMessage;
import com.github.WhiteMagic2014.gptApi.Embeddings.CreateEmbeddingsRequest;
import com.github.WhiteMagic2014.gptApi.Images.CreateImageRequest;
import com.github.WhiteMagic2014.util.Distance;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Gmp {

    private Map<String, Queue<ChatLog>> logs = new HashMap<>(); // 对话上下文
    private Map<String, String> personality = new HashMap<>(); //性格设定

    private int maxLog = 5; // 最大上下文层数

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

    public String originChat(List<ChatMessage> messages, int maxTokens, boolean stream) {
        CreateChatCompletionRequest request = new CreateChatCompletionRequest()
                .key(key)
                .maxTokens(maxTokens);
        if (StringUtils.isNotBlank(server)) {
            request.server(server);
        }
        if (StringUtils.isNotBlank(org)) {
            request.organization(org);
        }
        for (ChatMessage msg : messages) {
            request.addMessage(msg.getRole(), msg.getContent());
        }
        String result = "";
        try {
            if (stream) {
                result = streamRequest(request);
            } else {
                result = request.sendForChoices().get(0).getMessage().getContent();
            }
        } catch (Exception e) {
            return "很抱歉，出错了";
        }
        return result.trim();
    }

    public String originChat(List<ChatMessage> messages) {
        return originChat(messages, 500, false);
    }


    public String chat(String session, String prompt, int maxTokens, boolean stream) {
        String personal = personality.getOrDefault(session, "与用户进行闲聊或娱乐性的对话，以改善用户体验。");
        // 构造初始请求
        CreateChatCompletionRequest request = new CreateChatCompletionRequest()
                .key(key)
                .maxTokens(maxTokens)
                .addMessage("system", personal);
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
            if (stream) {
                result = streamRequest(request);
            } else {
                result = request.sendForChoices().get(0).getMessage().getContent();
            }
        } catch (Exception e) {
            try {
                JSONObject js = JSONObject.parseObject(e.getMessage());
                // 如果是长度超了。 遗忘一段记忆
                if (js.getJSONObject("error").getString("code").equals("context_length_exceeded")) {
                    if (logs.containsKey(session)) {
                        Queue<ChatLog> queue = logs.get(session);
                        queue.poll();
                        // 再次请求
                        return chat(session, prompt, maxTokens, stream);
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


    public String chat(String session, String prompt) {
        return chat(session, prompt, 500, false);
    }


    public String streamRequest(CreateChatCompletionRequest request) {
        StringBuilder sb = new StringBuilder();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        request.stream(true).outputStream(baos).send();
        byte[] data = baos.toByteArray();
        if (data.length > 0) {
            String result = new String(data);
            baos.reset();

            // 用正则截取 效率更高
            String pattern = "(?<=\"content\":\").*?(?=\\\"})";
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(result);
            while (matcher.find()) {
                sb.append(matcher.group(0).replace("\\n", "\n").replace("\\r", "\r"));
            }

            // 转jsonArray提取
//            String str = "[" + result.replace("data: [DONE]", "").replace("data:", ",") + "]";
//            JSONArray jsonArray = JSON.parseArray(str);
//            for (int i = 0; i < jsonArray.size(); i++) {
//                JSONObject choice = jsonArray.getJSONObject(i).getJSONArray("choices").getJSONObject(0);
//                if ("stop".equals(choice.getString("finish_reason"))) {
//                    break;
//                }
//                JSONObject delta = choice.getJSONObject("delta");
//                if (delta.containsKey("content")) {
//                    sb.append(delta.getString("content"));
//                }
//            }
        }
        return sb.toString();
    }


    /**
     * 设置 对话性格
     *
     * @param session
     * @param setting
     * @return
     */
    public String setPersonality(String session, String setting) {
        personality.put(session, setting);
        return "已经设定为: " + setting;
    }


    /**
     * 清除记忆上下文
     *
     * @param session
     * @return
     */
    public String clearLog(String session) {
        logs.remove(session);
        return "操作成功";
    }

    /**
     * 增添记忆上下文
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

    /**
     * 设置记忆上下文
     *
     * @param session
     * @param chatLogs
     */
    public void setChatLog(String session, Queue<ChatLog> chatLogs) {
        logs.put(session, chatLogs);
    }

    /**
     * 作图
     *
     * @param prompt
     * @param n
     * @return
     */
    public List<String> image(String prompt, int n) {
        CreateImageRequest request = new CreateImageRequest()
                .key(key)
                .prompt(prompt)
                .n(n)
                .largeSize();
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


    /**
     * 文本转向量
     *
     * @param inputs
     * @return
     */
    public List<List<Double>> input2Vector(List<String> inputs) {
        CreateEmbeddingsRequest request = new CreateEmbeddingsRequest()
                .key(key);
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


    /**
     * 文本转向量,向量是base64格式，便于存储
     *
     * @param inputs
     * @return
     */
    public List<String> input2VectorBase64(List<String> inputs) {
        CreateEmbeddingsRequest request = new CreateEmbeddingsRequest()
                .key(key)
                .base64Embedding(true);
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
        return request.sendForEmbeddingsBase64();
    }


    /**
     * 根据训练集 回答问题
     *
     * @param question       问题
     * @param dataEmbeddings 训练集
     * @param vectorNum      使用数据片段数量
     * @return
     */
    public String answer(String question, List<DataEmbedding> dataEmbeddings, int vectorNum) {
        if (dataEmbeddings.isEmpty()) {
            return "无预训练数据";
        }
        List<Double> questionEmbedding = input2Vector(Collections.singletonList(question)).get(0);
        List<DataEmbedding> sorted = dataEmbeddings.parallelStream()
                .peek(de -> de.setEmbeddingWithQuery(Distance.cosineDistance(questionEmbedding, de.getContextEmbedding())))
                .sorted(Comparator.comparing(DataEmbedding::getEmbeddingWithQuery).reversed())
                .collect(Collectors.toList());
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.systemMessage("根据下面的参考回答问题，请直接回答问题，如果无法回答问题，回答“我不知道”。"));
        StringBuilder prompt = new StringBuilder("参考:\n");
        for (int i = 0; i < Math.min(vectorNum, sorted.size()); i++) {
            prompt.append(sorted.get(i).getContext());
        }
        prompt.append("\n问题:\n").append(question);
        messages.add(ChatMessage.userMessage(prompt.toString()));
        return originChat(messages);
    }

    public String answer(String question, List<DataEmbedding> dataEmbeddings) {
        return answer(question, dataEmbeddings, 3);
    }


}