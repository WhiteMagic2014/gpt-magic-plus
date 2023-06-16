package com.github.WhiteMagic2014.util;

import com.github.WhiteMagic2014.gptApi.Embeddings.CreateEmbeddingsRequest;

import java.util.List;

public class VectorUtil {

    /**
     * 文本转向量
     *
     * @param input
     * @return
     */
    public static List<Double> input2Vector(String input) {
        CreateEmbeddingsRequest request = new CreateEmbeddingsRequest();
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
     * 文本转向量 base64
     *
     * @param input
     * @return
     */
    public static String input2VectorBase64(String input) {
        CreateEmbeddingsRequest request = new CreateEmbeddingsRequest()
                .base64Embedding(true);
        request.input(input);
        boolean flag;
        List<String> tmp = null;
        do {
            try {
                tmp = request.sendForEmbeddingsBase64();
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
     * 文本转向量
     *
     * @param inputs
     * @return
     */
    public static List<List<Double>> input2Vector(List<String> inputs) {
        CreateEmbeddingsRequest request = new CreateEmbeddingsRequest();
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
    public static List<String> input2VectorBase64(List<String> inputs) {
        CreateEmbeddingsRequest request = new CreateEmbeddingsRequest()
                .base64Embedding(true);
        if (inputs.size() == 1) {
            request.input(inputs.get(0));
        } else {
            String[] ins = new String[inputs.size()];
            inputs.toArray(ins);
            request.inputs(ins);
        }
        return request.sendForEmbeddingsBase64();
    }


}
