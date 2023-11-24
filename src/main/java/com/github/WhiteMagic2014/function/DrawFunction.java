package com.github.WhiteMagic2014.function;

import com.alibaba.fastjson.JSONObject;
import com.github.WhiteMagic2014.gptApi.GptModel;
import com.github.WhiteMagic2014.gptApi.Images.CreateImageRequest;
import com.github.WhiteMagic2014.gptApi.Images.pojo.OpenAiImage;
import com.github.WhiteMagic2014.tool.FunctionTool;
import com.github.WhiteMagic2014.tool.GptFunction;

import java.util.Collections;

public class DrawFunction extends GmpFunction {

    @Override
    public String getName() {
        return "drawPic";
    }

    @Override
    public FunctionTool getFunctionTool() {
        GptFunction function = new GptFunction();
        function.setName(getName());
        function.setDescription("根据用户的描述生成图片");
        JSONObject fp = new JSONObject();
        fp.put("type", "object");
        fp.put("required", Collections.singletonList("prompt"));
        JSONObject p = new JSONObject();
        JSONObject iprompt = new JSONObject();
        iprompt.put("type", "string");
        iprompt.put("description", "用来生成图片的描述");
        p.put("prompt", iprompt);
        fp.put("properties", p);
        function.setParameters(fp);
        return FunctionTool.functionTool(function);
    }

    @Override
    public HandleResult handle(JSONObject arguments) {
        String prompt = arguments.getString("prompt");
        String result;
        try {
            OpenAiImage image = new CreateImageRequest().prompt(prompt)
                    .model(GptModel.Dall_E_3)
                    .styleVivid()
                    .size1024x1024().
                    formatUrl().sendForImages().get(0);
            result = "图片地址: \n" + image.getUrl() + "\n语句:\n" + image.getRevised_prompt();
        } catch (Exception e) {
            result = "很抱歉,作图的时候出了一点问题,可以稍后再次尝试";
        }
        return new HandleResult(true, result);
    }
}
