package com.github.WhiteMagic2014;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
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

    private ContextMemory contextMemory;

    private int maxTokens = 500;// 回答问题限制的 token数量

    private boolean stream = false;

    private String model;

    public Gmp() {
        this.contextMemory = new DefaultContextMemory(5);
    }

    public Gmp(IndexSearcher indexSearcher) {
        this.indexSearcher = indexSearcher;
        this.contextMemory = new DefaultContextMemory(5);
    }

    public Gmp(IndexSearcher indexSearcher, ContextMemory contextMemory) {
        this.indexSearcher = indexSearcher;
        this.contextMemory = contextMemory;
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

    public String originChat(List<ChatMessage> messages, int maxTokens) {
        CreateChatCompletionRequest request = new CreateChatCompletionRequest()
                .maxTokens(maxTokens);
        if (StringUtils.isNotBlank(model)) {
            request.model(model);
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
        return originChat(messages, maxTokens);
    }


    public String chat(String session, String prompt, int maxTokens) {
        String personal = contextMemory.getPersonality(session);
        // 构造初始请求
        CreateChatCompletionRequest request = new CreateChatCompletionRequest()
                .maxTokens(maxTokens)
                .addMessage(ChatMessage.systemMessage(personal));
        if (StringUtils.isNotBlank(model)) {
            request.model(model);
        }
        // 拼接历史对话记录
        Queue<ChatLog> queue = contextMemory.chatLogs(session);
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
                result = streamRequest(request);
            } else {
                result = request.sendForChoices().get(0).getMessage().getContent();
            }
        } catch (Exception e) {
            try {
                JSONObject js = JSONObject.parseObject(e.getMessage());
                // 如果是长度超了。 遗忘一段记忆
                if (js.getJSONObject("error").getString("code").equals("context_length_exceeded")) {
                    if (queue != null && !queue.isEmpty()) {
                        queue.poll();
                    }
                    // 再次请求
                    return chat(session, prompt, maxTokens);
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
        return chat(session, prompt, maxTokens);
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
        contextMemory.setPersonality(session, setting);
        return "已经设定为: " + setting;
    }


    /**
     * 清除记忆上下文
     *
     * @param session
     * @return
     */
    public String clearLog(String session) {
        contextMemory.clearChatLogs(session);
        return "操作成功";
    }

    /**
     * 增添记忆上下文
     */
    public void addChatLog(String session, String user, String assistant) {
        contextMemory.addChatLog(session, user, assistant);
    }

    /**
     * 设置记忆上下文
     *
     * @param session
     * @param chatLogs
     */
    public void setChatLog(String session, Queue<ChatLog> chatLogs) {
        contextMemory.setChatLog(session, chatLogs);
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
     * 现在不再推荐在 answer中直接给出全量向量集合，推荐 搭配IndexSearcher 使用
     * 根据相似度较高的几个(vectorNum)切片数据 回答问题
     *
     * @param question          问题
     * @param dataEmbeddingPool 全量训练集, 根据问题计算相似度取前vectorNum个使用
     * @param vectorNum         使用数据片段数量
     * @return
     */
    @Deprecated
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
        return originChat(messages, maxTokens);
    }


    /**
     * 根据预训练数据回答问题,依赖初始化gmp时设置的 indexSearcher,
     *
     * @param session  session id 可以保证上下文连贯
     * @param question 问题
     * @return
     */
    public QuestionAnswer answer(String session, String question) throws Exception {
        if (indexSearcher == null) {
            throw new RuntimeException("indexSearcher 未配置");
        }
        List<DataIndex> indices = indexSearcher.search(question);
        return answer(session, question, indices);
    }


    /**
     * 根据预训练数据回答问题,依赖提供的 indexSearcher
     *
     * @param session       session id 可以保证上下文连贯
     * @param question      问题
     * @param indexSearcher 向量搜索器
     * @return
     */
    public QuestionAnswer answer(String session, String question, IndexSearcher indexSearcher) throws Exception {
        if (indexSearcher == null) {
            throw new RuntimeException("indexSearcher 未配置");
        }
        List<DataIndex> indices = indexSearcher.search(question);
        return answer(session, question, indices);
    }


    /**
     * 根据直接给出的 DataIndex 构造轮训归纳问答 ，
     * 和 String answer(String question, List<T> dataEmbeddingPool, int vectorNum) 不同，这里将寻找相似向量的工作解耦，实际使用下会更自由
     *
     * @param session  session id 可以保证上下文连贯
     * @param question 问题
     * @param indices  相关切片
     * @return
     * @throws Exception
     */
    public QuestionAnswer answer(String session, String question, List<DataIndex> indices) throws Exception {
        CreateChatCompletionRequest request = new CreateChatCompletionRequest()
                .maxTokens(maxTokens);
        if (StringUtils.isNotBlank(model)) {
            request.model(model);
        }
        // 历史对话记录 预处理存档
        List<ChatMessage> chatLog = new ArrayList<>();
        Queue<ChatLog> queue = contextMemory.chatLogs(session);
        if (queue != null && !queue.isEmpty()) {
            queue.forEach(l -> {
                chatLog.add(ChatMessage.userMessage(l.getUser()));
                chatLog.add(ChatMessage.assistantMessage(l.getAssistant()));
            });
        }
        QuestionAnswer answer = new QuestionAnswer();
        // 构造问答
        answer.setRounds(indices.size());
        // 中间答案
        String result = "";
        for (int i = 0; i < indices.size(); i++) {
            DataIndex index = indices.get(i);
            // 历史上下文
            List<ChatMessage> chatLogTmp = new ArrayList<>(chatLog);
            // 本轮问题的 prompt
            List<ChatMessage> questionRounds = new ArrayList<>();
            if (i == 0) {
                String promptTemplate = "上下文信息如下\n----------------\n{context}\n----------------\n给定上下文信息而非先验知识，回答以下问题：\n{question}\n";
                questionRounds.add(ChatMessage.userMessage(promptTemplate.replace("{context}", index.getContext()).replace("{question}", question)));
            } else {
                String promptTemplate = "我们有机会（仅在必要时）通过下面的更多上下文来完善上述答案。\n----------------\n{context}\n----------------\n在新的背景下，完善原始答案以更好地回答问题。如果上下文没有用处，请再次输出原始答案。";
                questionRounds.add(ChatMessage.userMessage(question));
                questionRounds.add(ChatMessage.assistantMessage(result));
                questionRounds.add(ChatMessage.userMessage(promptTemplate.replace("{context}", index.getContext())));
            }
            // 获得答案
            boolean flag;
            do {
                // 组合prompt 上下文+prompt
                List<ChatMessage> requestMessage = new ArrayList<>();
                requestMessage.addAll(chatLogTmp);
                requestMessage.addAll(questionRounds);
                request.messages(new ArrayList<>(requestMessage));
                try {
                    if (stream) {
                        result = streamRequest(request);
                    } else {
                        result = request.sendForChoices().get(0).getMessage().getContent();
                    }
                    flag = false;
                } catch (Exception e) {
                    try {
                        JSONObject js = JSONObject.parseObject(e.getMessage());
                        // 如果是长度超了。 遗忘一段记忆，这里需要重新构造request
                        if (js.getJSONObject("error").getString("code").equals("context_length_exceeded")) {
                            // 还有东西能遗忘,遗忘前两条数据（一组问答）
                            if (chatLogTmp.size() > 2) {
                                chatLogTmp.remove(0);
                                chatLogTmp.remove(0);
                            } else {
                                // 没东西能忘记了，说明单单本次的prompt就超了，没办法处理
                                throw new Exception("prompt 过长无法完成本次请求");
                            }
                        }
                    } catch (JSONException je) {
                        je.printStackTrace();
                    }
                    flag = true;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                    }
                }
            } while (flag);
            JSONObject source = JSONObject.parseObject(index.getSource());
            source.put("content", index.getContext());
            answer.addSource(source.toJSONString());
            answer.addTempResult(result);
        }
        if (StringUtils.isNotBlank(result)) {
            addChatLog(session, question, result);
        }
        answer.setResult(result);
        return answer;
    }

}