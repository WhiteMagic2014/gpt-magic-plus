package com.github.WhiteMagic2014.gmp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.WhiteMagic2014.beans.DataIndex;
import com.github.WhiteMagic2014.gptApi.Chat.CreateChatCompletionRequest;
import com.github.WhiteMagic2014.gptApi.Chat.pojo.ChatMessage;
import com.github.WhiteMagic2014.util.VectorUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

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

    private String storage;// 生成index的存储地址

    private int sliceSize = 512;//数据切片大小

    private boolean autoTags = false;//是否自动给切片生成标签

    private boolean sliceByParagraph = false;// 切片模式

    public IndexCreator(String storage) {
        this.storage = storage;
    }

    /**
     * @param sliceSize 限制每片切片大小
     * @return
     */
    public IndexCreator sliceSize(int sliceSize) {
        this.sliceSize = sliceSize;
        return this;
    }

    /**
     * @param autoTags 是否自动打标签（gpt打标签，效果不一定好，复杂的内容建议不要这样）
     * @return
     */
    public IndexCreator autoTags(boolean autoTags) {
        this.autoTags = autoTags;
        return this;
    }

    /**
     * true = 根据段落分割。 避免了将一段文本从中切断造成断章取义的情况，但因为是按段落切片，会发生单个段落长度超过限制size的情况，对输入的文本有一定要求
     * false = 根据字数限制 。会严格控制每个切片的长度
     *
     * @param sliceByParagraph 切片模式
     */
    public IndexCreator sliceByParagraph(boolean sliceByParagraph) {
        this.sliceByParagraph = sliceByParagraph;
        return this;
    }

    /**
     * @param sliceSize 限制每片切片大小
     * @return
     */
    public void setSliceSize(int sliceSize) {
        this.sliceSize = sliceSize;
    }


    /**
     * @param autoTags 是否自动打标签（gpt打标签，效果不一定好，复杂的内容建议不要这样）
     * @return
     */
    public void setAutoTags(boolean autoTags) {
        this.autoTags = autoTags;
    }

    /**
     * true = 根据段落分割。 避免了将一段文本从中切断造成断章取义的情况，但因为是按段落切片，会发生单个段落长度超过限制size的情况，对输入的文本有一定要求
     * false = 根据字数限制 。会严格控制每个切片的长度
     *
     * @param sliceByParagraph 切片模式
     */
    public void setSliceByParagraph(boolean sliceByParagraph) {
        this.sliceByParagraph = sliceByParagraph;
    }

    /**
     * @param context 文本内容
     * @param source  数据来源 比如说文本是一个文件的内容，那就可以写文件名
     * @return
     */
    public List<DataIndex> createIndex(String context, String source) {
        return createIndex(context, source, false);
    }


    /**
     * @param context 文本内容
     * @param source  数据来源 比如说文本是一个文件的内容，那就可以写文件名
     * @param base64  向量格式是否使用base64
     * @return
     */
    public List<DataIndex> createIndex(String context, String source, Boolean base64) {
        String fileName = createFile(source);
        List<DataIndex> dataIndices = new ArrayList<>();
        String uuid = UUID.randomUUID().toString();
        List<String> contextPieces;
        if (sliceByParagraph) {
            contextPieces = sliceContextParagraph(context, sliceSize);
        } else {
            contextPieces = sliceContext(context, sliceSize);
        }
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
            sourceJson.put("no", (i + 1));
            tmp.setSource(sourceJson.toJSONString());
            // embedding
            tmp.setContext(contextPiece);
            if (base64) {
                String embedding = VectorUtil.input2VectorBase64(contextPiece);
                tmp.setContextEmbeddingB64(embedding);
                tmp.setBase64Embedding(true);
            } else {
                List<Double> embedding = VectorUtil.input2Vector(contextPiece);
                tmp.setContextEmbedding(embedding);
                tmp.setBase64Embedding(false);
            }
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
        saveFile(fileName, dataIndices);
        return dataIndices;
    }


    /**
     * @param pdfFilePath pdf文件地址
     * @return
     */
    public List<DataIndex> createIndexPdf(String pdfFilePath) {
        return createIndexPdf(pdfFilePath, false);
    }

    /**
     * @param pdfFilePath pdf文件地址
     * @param base64      向量格式是否使用base64
     * @return
     */
    public List<DataIndex> createIndexPdf(String pdfFilePath, Boolean base64) {
        try {
            File pdfFile = new File(pdfFilePath);
            PDDocument document = PDDocument.load(pdfFile);
            List<DataIndex> dataIndices = new ArrayList<>();
            String fileName = createFile(pdfFile.getName());
            String uuid = UUID.randomUUID().toString();
            int no = 1;
            PDFTextStripper stripper = new PDFTextStripper();
            for (int i = 1; i <= document.getNumberOfPages(); i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                List<String> contextPieces;
                if (sliceByParagraph) {
                    contextPieces = sliceContextParagraph(stripper.getText(document), sliceSize);
                } else {
                    contextPieces = sliceContext(stripper.getText(document), sliceSize);
                }
                for (String contextPiece : contextPieces) {
                    DataIndex tmp = new DataIndex();
                    tmp.setId(uuid + "_" + no);
                    // node
                    tmp.setBeforeId(uuid + "_" + (no - 1));
                    tmp.setAfterId(uuid + "_" + (no + 1));
                    // source
                    JSONObject sourceJson = new JSONObject();
                    sourceJson.put("source", pdfFile.getName());
                    sourceJson.put("sliceSize", sliceSize);
                    sourceJson.put("no", no);
                    sourceJson.put("page", i);
                    tmp.setSource(sourceJson.toJSONString());
                    // embedding
                    tmp.setContext(contextPiece);
                    if (base64) {
                        String embedding = VectorUtil.input2VectorBase64(contextPiece);
                        tmp.setContextEmbeddingB64(embedding);
                        tmp.setBase64Embedding(true);
                    } else {
                        List<Double> embedding = VectorUtil.input2Vector(contextPiece);
                        tmp.setContextEmbedding(embedding);
                        tmp.setBase64Embedding(false);
                    }
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
                    no++;
                }
            }
            document.close();
            // 去除头尾节点的 上下节点
            dataIndices.get(0).setBeforeId(null);
            dataIndices.get(dataIndices.size() - 1).setAfterId(null);
            // 保存数据为文件
            saveFile(fileName, dataIndices);
            return dataIndices;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }


    /**
     * 根据source 创建 文件
     *
     * @param source
     * @return
     */
    private String createFile(String source) {
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
        return fileName;
    }


    /**
     * 将 List<DataIndex> 转换为json 存储至文件
     *
     * @param fileName
     * @param dataIndices
     */
    private void saveFile(String fileName, List<DataIndex> dataIndices) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
            for (DataIndex di : dataIndices) {
                String json = JSON.toJSONString(di);
                bw.write(json);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 文本打标签
     *
     * @param context
     * @return
     */
    private List<String> tagContext(String context) {
        CreateChatCompletionRequest request = new CreateChatCompletionRequest()
                .maxTokens(128)
                .temperature(0.0f);
        request.addMessage(ChatMessage.systemMessage("是你一个数据标记员，负责给数据做标记"));
        request.addMessage(ChatMessage.userMessage("请给以下内容1-3个相关的标签,如果有多个标签,用'&'符号隔开:\n" + context));
        String tmp = (String) request.sendForChoices().get(0).getMessage().getContent();
        return Arrays.stream(tmp.split("&")).map(String::trim).filter(StringUtils::isNotBlank).collect(Collectors.toList());
    }


    /**
     * 文本切片
     *
     * @param context 文本
     * @param size    每片大小
     * @return
     */
    public static List<String> sliceContext(String context, int size) {
        context = context
                .replace(" ", "")
                .replace(" ", "")
                .replaceAll("(\\n\\r|\\n)+", "\n");
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


    /**
     * 文本按段落切片(因为是按段落切片，会发生单个段落长度超过限制size的情况，对输入的文本有一定要求)
     *
     * @param context 文本
     * @param size    每片大小
     * @return
     */
    public static List<String> sliceContextParagraph(String context, int size) {
        context = context
                .replace(" ", "")
                .replace(" ", "")
                .replaceAll("(\\n\\r|\\n)+", "\n");

        String[] lines = context.split("\n");
        List<String> result = new ArrayList<>();
        StringBuilder temp = new StringBuilder();
        for (int i = 0; i < lines.length; ) {
            String line = lines[i];
            if ((temp.length() + line.length()) <= size) {
                temp.append(line).append("\n");
                i++;
            } else {
                if (temp.length() > 0) {
                    result.add(temp.toString());
                    temp.delete(0, temp.length());
                } else {
                    result.add(line);
                    i++;
                }
            }
        }
        if (temp.length() > 0) {
            result.add(temp.toString());
        }
        return result;
    }

}