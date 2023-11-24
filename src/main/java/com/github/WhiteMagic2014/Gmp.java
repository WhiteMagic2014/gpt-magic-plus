package com.github.WhiteMagic2014;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.github.WhiteMagic2014.beans.ChatLog;
import com.github.WhiteMagic2014.beans.DataEmbedding;
import com.github.WhiteMagic2014.beans.DataIndex;
import com.github.WhiteMagic2014.beans.QuestionAnswer;
import com.github.WhiteMagic2014.function.GmpFunction;
import com.github.WhiteMagic2014.gptApi.Chat.CreateChatCompletionRequest;
import com.github.WhiteMagic2014.gptApi.Chat.pojo.ChatMessage;
import com.github.WhiteMagic2014.gptApi.GptModel;
import com.github.WhiteMagic2014.gptApi.Images.CreateImageRequest;
import com.github.WhiteMagic2014.gptApi.Images.pojo.OpenAiImage;
import com.github.WhiteMagic2014.tool.FunctionTool;
import com.github.WhiteMagic2014.util.Distance;
import com.github.WhiteMagic2014.util.EmbeddingUtil;
import com.github.WhiteMagic2014.util.RequestUtil;
import com.github.WhiteMagic2014.util.VectorUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Gmp {

    private IndexSearcher indexSearcher;

    private ContextMemory contextMemory;

    private int maxTokens = 500;// 回答问题限制的 token数量

    private boolean stream = false;

    private String model;

    private String answerPromptTemplate = "上下文信息如下\n----------------\n${context}\n----------------\n给定上下文信息而非先验知识，回答以下问题：\n${question}\n";
    private String answerOptimizePromptTemplate = "我们有机会（仅在必要时）通过下面的更多上下文来完善上述答案。\n----------------\n${context}\n----------------\n在新的背景下，完善原始答案以更好地回答问题。如果上下文没有用处，请再次输出原始答案。";


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


    /**
     * @param answerPromptTemplate 首次问答prompt
     */
    public void setAnswerPromptTemplate(String answerPromptTemplate) {
        this.answerPromptTemplate = answerPromptTemplate;
    }

    /**
     * @param answerOptimizePromptTemplate 后续根据额外数据优化prompt
     */
    public void setAnswerOptimizePromptTemplate(String answerOptimizePromptTemplate) {
        this.answerOptimizePromptTemplate = answerOptimizePromptTemplate;
    }

    public String originChat(List<ChatMessage> messages, int maxTokens) {
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

    public String originChat(List<ChatMessage> messages) {
        return originChat(messages, maxTokens);
    }


    public String chat(String session, ChatMessage userMessage, String model, int maxTokens, List<GmpFunction> gmpFunction) {
        // 有函数
        boolean hasFunction = (gmpFunction != null) && (!gmpFunction.isEmpty());

        String personal = contextMemory.getPersonality(session);
        // 构造初始请求
        CreateChatCompletionRequest request = new CreateChatCompletionRequest()
                .maxTokens(maxTokens)
                .addMessage(ChatMessage.systemMessage(personal));
        if (hasFunction) {
            List<FunctionTool> functionTools = gmpFunction.stream().map(GmpFunction::getFunctionTool).collect(Collectors.toList());
            request.tools(functionTools);
        }
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
        request.addMessage(userMessage);
        // 发送请求
        String result = "";
        try {
            if (stream && !hasFunction) {
                result = RequestUtil.streamRequest(request);
            } else {
                ChatMessage message = request.sendForChoices().get(0).getMessage();
                if (message.getTool_calls() != null) {
                    // 函数调用 走内部代理
                    Map<String, GmpFunction> handleMap = gmpFunction.stream().collect(Collectors.toMap(GmpFunction::getName, Function.identity()));
                    JSONObject functionJson = message.getTool_calls().getJSONObject(0).getJSONObject("function");
                    result = handleMap.get(functionJson.getString("name")).handleToolMessage(userMessage, message);
                } else {
                    result = (String) request.sendForChoices().get(0).getMessage().getContent();
                }
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
                    return chat(session, userMessage, model, maxTokens);
                } else {
                    return code;
                }
            } catch (Exception je) {
                return e.getMessage();
            }
        }
        // 记忆上下文
        Object content = userMessage.getContent();
        StringBuilder contentStr = new StringBuilder();
        if (content instanceof String) {
            contentStr = new StringBuilder((String) content);
        } else if (content instanceof ArrayList) {
            ArrayList<JSONObject> t = (ArrayList<JSONObject>) content;
            for (JSONObject j : t) {
                if (j.get("type").equals("text")) {
                    contentStr.append(j.getString("text"));
                }
            }
        }
        addChatLog(session, contentStr.toString(), result);
        return result;
    }

    public String chat(String session, ChatMessage userMessage, String model, int maxTokens) {
        return chat(session, userMessage, model, maxTokens, null);
    }

    public String chat(String session, String prompt, int maxTokens) {
        return chat(session, ChatMessage.userMessage(prompt), model, maxTokens, null);
    }

    public String chat(String session, String prompt) {
        return chat(session, ChatMessage.userMessage(prompt), model, maxTokens, null);
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
     * @return
     */
    public String image(String prompt) {
        CreateImageRequest request = new CreateImageRequest()
                .model(GptModel.Dall_E_3)
                .styleVivid()
                .prompt(prompt)
                .size1024x1024();
        List<OpenAiImage> temp = null;
        try {
            temp = request.sendForImages();
        } catch (Exception e) {
            return "出错了";
        }
        return temp.get(0).getUrl();
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
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < Math.min(vectorNum, sorted.size()); i++) {
            context.append(sorted.get(i).getContext()).append("\n");
        }
        messages.add(ChatMessage.userMessage(answerPromptTemplate.replace("${context}", context).replace("${question}", question)));
        return originChat(messages);
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
                questionRounds.add(ChatMessage.userMessage(answerPromptTemplate.replace("${context}", index.getContext()).replace("${question}", question)));
            } else {
                questionRounds.add(ChatMessage.userMessage(question));
                questionRounds.add(ChatMessage.assistantMessage(result));
                questionRounds.add(ChatMessage.userMessage(answerOptimizePromptTemplate.replace("${context}", index.getContext())));
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
                        result = RequestUtil.streamRequest(request);
                    } else {
                        result = (String) request.sendForChoices().get(0).getMessage().getContent();
                    }
                    flag = false;
                } catch (Exception e) {
                    try {
                        JSONObject js = JSONObject.parseObject(e.getMessage());
                        String code = js.getJSONObject("error").getString("code");
                        // 如果是长度超了。 遗忘一段记忆，这里需要重新构造request
                        if (code.equals("context_length_exceeded")) {
                            // 还有东西能遗忘,遗忘前两条数据（一组问答）
                            if (chatLogTmp.size() > 2) {
                                chatLogTmp.remove(0);
                                chatLogTmp.remove(0);
                            } else {
                                // 没东西能忘记了，说明单单本次的prompt就超了，没办法处理
                                throw new Exception("prompt 过长无法完成本次请求");
                            }
                        } else {
                            throw new Exception(code);
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