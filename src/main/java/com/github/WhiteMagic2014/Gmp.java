package com.github.WhiteMagic2014;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.WhiteMagic2014.beans.ChatLog;
import com.github.WhiteMagic2014.beans.DataEmbedding;
import com.github.WhiteMagic2014.beans.DataIndex;
import com.github.WhiteMagic2014.beans.QuestionAnswer;
import com.github.WhiteMagic2014.gptApi.Chat.CreateChatCompletionRequest;
import com.github.WhiteMagic2014.gptApi.Chat.pojo.ChatMessage;
import com.github.WhiteMagic2014.gptApi.Images.CreateImageRequest;
import com.github.WhiteMagic2014.util.Distance;
import com.github.WhiteMagic2014.util.EmbeddingUtil;
import com.github.WhiteMagic2014.util.VectorUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Gmp {

    private IndexSearcher indexSearcher;

    private Map<String, Queue<ChatLog>> logs = new HashMap<>(); // 对话上下文
    private Map<String, String> personality = new HashMap<>(); //性格设定

    private int maxLog = 5; // 最大上下文层数

    private int maxTokens = 500;// 回答问题限制的 token数量


    public Gmp() {
    }

    public Gmp(IndexSearcher indexSearcher) {
        this.indexSearcher = indexSearcher;
    }

    public void setMaxLog(int maxLog) {
        this.maxLog = maxLog;
    }


    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public String originChat(List<ChatMessage> messages, int maxTokens, boolean stream) {
        CreateChatCompletionRequest request = new CreateChatCompletionRequest()
                .maxTokens(maxTokens);
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
                .maxTokens(maxTokens)
                .addMessage(ChatMessage.systemMessage(personal));
        // 拼接历史对话记录
        if (logs.containsKey(session)) {
            Queue<ChatLog> queue = logs.get(session);
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
                .prompt(prompt)
                .n(n)
                .largeSize();
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
     * 根据相似度较高的几个(vectorNum)切片数据 回答问题
     *
     * @param question          问题
     * @param dataEmbeddingPool 全量训练集, 根据问题计算相似度取前vectorNum个使用
     * @param vectorNum         使用数据片段数量
     * @return
     */
    public <T extends DataEmbedding> String answer(String question, List<T> dataEmbeddingPool, int vectorNum) {
        if (dataEmbeddingPool.isEmpty()) {
            return "无预训练数据";
        }
        List<Double> questionEmbedding = VectorUtil.input2Vector(question);
        List<DataEmbedding> sorted = dataEmbeddingPool.parallelStream()
                .peek(de -> {
                    if (de.getBase64Embedding()) {
                        de.setEmbeddingWithQuery(Distance.cosineDistance(questionEmbedding, EmbeddingUtil.embeddingB64ToDoubleList(de.getContextEmbeddingB64())));
                    } else {
                        de.setEmbeddingWithQuery(Distance.cosineDistance(questionEmbedding, de.getContextEmbedding()));
                    }
                })
                .sorted(Comparator.comparing(DataEmbedding::getEmbeddingWithQuery).reversed())
                .collect(Collectors.toList());
        List<ChatMessage> messages = new ArrayList<>();
        StringBuilder prompt = new StringBuilder("下面是上下文信息\n------------\n");
        for (int i = 0; i < Math.min(vectorNum, sorted.size()); i++) {
            prompt.append(sorted.get(i).getContext());
        }
        prompt.append(" \n------------\n根据上下文信息而非先前知识，回答问题：").append(question);
        messages.add(ChatMessage.userMessage(prompt.toString()));
        return originChat(messages);
    }


    /**
     * 根据预训练数据回答问题,依赖 indexSearcher
     *
     * @param session  session id 可以保证上下文连贯
     * @param question 问题
     * @return
     */
    public QuestionAnswer answer(String session, String question) {
        if (indexSearcher == null) {
            throw new RuntimeException("indexSearcher 未配置");
        }
        CreateChatCompletionRequest request = new CreateChatCompletionRequest()
//                .model(GptModel.gpt_3p5_turbo_16k)
                .maxTokens(maxTokens);
        // 历史对话记录 预处理存档
        List<ChatMessage> chatLog = new ArrayList<>();
        if (logs.containsKey(session)) {
            Queue<ChatLog> queue = logs.get(session);
            queue.forEach(l -> {
                chatLog.add(ChatMessage.userMessage(l.getUser()));
                chatLog.add(ChatMessage.assistantMessage(l.getAssistant()));
            });
        }
        QuestionAnswer answer = new QuestionAnswer();
        // 构造问答
        List<DataIndex> indices = indexSearcher.search(question);
        answer.setRounds(indices.size());
        // 中间答案
        String result = "";
        for (int i = 0; i < indices.size(); i++) {
            DataIndex index = indices.get(i);
            // 初始化本轮问答，如果有历史记录则填充
            if (chatLog.isEmpty()) {
                request.messages(new ArrayList<>());
            } else {
                request.messages(new ArrayList<>(chatLog));
            }
            if (i == 0) {
                String promptTemplate = "上下文信息如下\n----------------\n{context}\n----------------\n给定上下文信息而非先验知识，回答以下问题：\n{question}\n";
                request.addMessage(ChatMessage.userMessage(promptTemplate.replace("{context}", index.getContext()).replace("{question}", question)));
            } else {
                String promptTemplate = "我们有机会（仅在必要时）通过下面的更多上下文来完善上述答案。\n----------------\n{context}\n----------------\n在新的背景下，完善原始答案以更好地回答问题。如果上下文没有用处，请再次输出原始答案。";
                request.addMessage(ChatMessage.userMessage(question));
                request.addMessage(ChatMessage.assistantMessage(result));
                request.addMessage(ChatMessage.userMessage(promptTemplate.replace("{context}", index.getContext())));
            }
            // 获得答案
            boolean flag;
            do {
                try {
                    result = request.sendForChoices().get(0).getMessage().getContent();
                    flag = false;
                } catch (Exception e) {
                    flag = true;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            } while (flag);
            answer.addSource(index.getSource());
            answer.addTempResult(result);
        }
        if (StringUtils.isNotBlank(result)) {
            addChatLog(session, question, result);
        }
        answer.setResult(result);
        return answer;
    }

}