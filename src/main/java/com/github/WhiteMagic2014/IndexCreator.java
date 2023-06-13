package com.github.WhiteMagic2014;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.WhiteMagic2014.beans.DataIndex;
import com.github.WhiteMagic2014.gptApi.Chat.CreateChatCompletionRequest;
import com.github.WhiteMagic2014.gptApi.Chat.pojo.ChatMessage;
import com.github.WhiteMagic2014.gptApi.Embeddings.CreateEmbeddingsRequest;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class IndexCreator {

    private String server; // 代理服务器，默认为openai官方

    private String key; // openai key

    private String storage;// 生成index的存储地址

    private int sliceSize = 512;//数据切片大小

    private boolean autoTags = false;//是否自动给切片生成标签

    public IndexCreator(String key, String storage) {
        this.key = key;
        this.storage = storage;
    }

    public IndexCreator(String server, String key, String storage) {
        this.server = server;
        this.key = key;
        this.storage = storage;
    }

    public IndexCreator sliceSize(int sliceSize) {
        this.sliceSize = sliceSize;
        return this;
    }

    public IndexCreator autoTags(boolean autoTags) {
        this.autoTags = autoTags;
        return this;
    }

    public void setSliceSize(int sliceSize) {
        this.sliceSize = sliceSize;
    }

    public void setAutoTags(boolean autoTags) {
        this.autoTags = autoTags;
    }

    /**
     * @param context 文本内容
     * @param source  数据来源 比如说文本是一个文件的内容，那就可以写文件名
     * @return
     */
    public List<DataIndex> createIndex(String context, String source) {
        // 检查 storage 文件夹是否存在，不存在则创建
        File storageFolder = new File(storage);
        if (!storageFolder.exists()) {
            storageFolder.mkdirs();
        }
        // 将 storage+source+".gmpIndex" 作为文件名 fileName
        String fileName = storage + File.separator + source + ".gmpIndex";
        // 如果 storage 文件夹存在 并且其中已经存在了与fileName同名的文件， 把文件名改成  storage+source+".new.gmpIndex"
        File indexFile = new File(fileName);
        if (indexFile.exists()) {
            fileName = storage + File.separator + source + ".new.gmpIndex";
        }
        List<DataIndex> dataIndices = new ArrayList<>();
        String uuid = UUID.randomUUID().toString();
        List<String> contextPieces = sliceContext(context, sliceSize);
        for (int i = 0; i < contextPieces.size(); i++) {
            String contextPiece = contextPieces.get(i);
            DataIndex tmp = new DataIndex();
            tmp.setId(uuid + "_" + i);
            // node
            if (i > 0) {
                tmp.setBeforeId(uuid + "_" + (i - 1));
            }
            if (i < contextPieces.size() - 1) {
                tmp.setAfterId(uuid + "_" + (i + 1));
            }
            // source
            JSONObject sourceJson = new JSONObject();
            sourceJson.put("source", source);
            sourceJson.put("sliceSize", sliceSize);
            sourceJson.put("no", (i+1));
            tmp.setSource(sourceJson.toJSONString());
            // embedding
            tmp.setContext(contextPiece);
            List<Double> embedding = input2Vector(contextPiece);
            tmp.setContextEmbedding(embedding);
            // 自动打标记,不一定准,最好还是人为标记数据
            if (autoTags) {
                // 因为打标记本身也不是机器必须要做的事情，所以就尝试一次，如果因为各种原因没成功就不多次尝试了
                try {
                    tmp.setTags(tagContext(contextPiece));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            dataIndices.add(tmp);
        }
        // 将 List<DataIndex> 转换为json 存储至文件
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
            for (DataIndex di : dataIndices) {
                String json = JSON.toJSONString(di);
                bw.write(json);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataIndices;
    }


    /**
     * 文本打标签
     *
     * @param context
     * @return
     */
    private List<String> tagContext(String context) {
        CreateChatCompletionRequest request = new CreateChatCompletionRequest()
                .key(key)
                .maxTokens(128)
                .temperature(0.0f);
        if (StringUtils.isNotBlank(server)) {
            request.server(server);
        }
        request.addMessage(ChatMessage.systemMessage("是你一个数据标记员，负责给数据做标记"));
        request.addMessage(ChatMessage.userMessage("请给以下内容1-3个相关的标签,如果有多个标签,用'&'符号隔开:\n" + context));
        String tmp = request.sendForChoices().get(0).getMessage().getContent();
        return Arrays.stream(tmp.split("&")).map(String::trim).filter(StringUtils::isNotBlank).collect(Collectors.toList());
    }


    /**
     * 文本转向量
     *
     * @param input
     * @return
     */
    private List<Double> input2Vector(String input) {
        CreateEmbeddingsRequest request = new CreateEmbeddingsRequest()
                .key(key);
        if (StringUtils.isNotBlank(server)) {
            request.server(server);
        }
        request.input(input);
        boolean flag;
        List<List<Double>> tmp = null;
        do {
            try {
                tmp = request.sendForEmbeddings();
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
        return tmp.get(0);
    }


    /**
     * 文本切片
     *
     * @param context 文本
     * @param size    每片大小
     * @return
     */
    public static List<String> sliceContext(String context, int size) {
        List<String> result = new ArrayList<>();
        int start = 0;
        int end = size;
        StringBuilder sb = new StringBuilder();
        while (start < context.length()) {
            if (end >= context.length()) {
                end = context.length();
            }
            sb.setLength(0);
            for (int i = start; i < end; i++) {
                sb.append(context.charAt(i));
            }
            result.add(sb.toString());
            start = end;
            end += size;
        }
        return result;
    }

}