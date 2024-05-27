package com.github.WhiteMagic2014.gmpa;

import com.github.WhiteMagic2014.gptApi.Assistant.Thread.CreateThreadRequest;
import com.github.WhiteMagic2014.gptApi.Assistant.Thread.DeleteThreadRequest;
import com.github.WhiteMagic2014.gptApi.Assistant.Thread.Message.CreateMessageRequest;
import com.github.WhiteMagic2014.gptApi.Assistant.Thread.Message.ListMessagesRequest;
import com.github.WhiteMagic2014.gptApi.Assistant.Thread.Run.CreateRunRequest;
import com.github.WhiteMagic2014.gptApi.Assistant.Thread.Run.RetrieveRunRequest;
import com.github.WhiteMagic2014.gptApi.Assistant.pojo.ThreadMessage;
import com.github.WhiteMagic2014.gptApi.Assistant.pojo.ThreadRun;
import com.github.WhiteMagic2014.tool.resource.ToolResource;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @Description: Gmp Assistant
 * @author: magic chen
 * @date: 2023/11/21 10:51
 **/
public class GmpAssistant {

    private final String assistantId;
    private final String model;
    private final AssistantContext assistantContext;

    public GmpAssistant(String assistantId) {
        this.assistantId = assistantId;
        this.model = null;
        this.assistantContext = new DefaultAssistantContext();
    }

    public GmpAssistant(String assistantId, String model) {
        this.assistantId = assistantId;
        this.model = model;
        this.assistantContext = new DefaultAssistantContext();
    }

    public GmpAssistant(String assistantId, AssistantContext assistantContext) {
        this.assistantId = assistantId;
        this.model = null;
        this.assistantContext = assistantContext;
    }

    public GmpAssistant(String assistantId, String model, AssistantContext assistantContext) {
        this.assistantId = assistantId;
        this.model = model;
        this.assistantContext = assistantContext;
    }


    /**
     * 默认
     * 创建 对话 thread  不额外指定vectorStore使用assistant默认的
     *
     * @param session
     * @return
     */
    public String createThread(String session) {
        return createThread(session, null);
    }

    /**
     * 创建 对话 thread  指定vectorStore
     *
     * @param session
     * @param vectorStoreIds
     * @return
     */
    public String createThread(String session, List<String> vectorStoreIds) {
        String threadId;
        if (assistantContext.checkThreadId(session)) {
            threadId = assistantContext.getThreadId(session);
        } else {
            CreateThreadRequest req = new CreateThreadRequest();
            if (vectorStoreIds != null && !vectorStoreIds.isEmpty()) {
                req.toolResources(ToolResource.fileSearchResource(vectorStoreIds, null));
            }
            threadId = req.sendForGptThread().getId();
            assistantContext.setThreadId(session, threadId);
        }
        return threadId;
    }


    /**
     * 对话
     *
     * @param session
     * @param prompt
     * @return
     */
    public String chat(String session, String prompt) {
        return chat(session, prompt, assistantId);
    }


    /**
     * 对话
     *
     * @param session
     * @param prompt
     * @param assistantId 可以指定使用的assistant
     * @return
     */
    public String chat(String session, String prompt, String assistantId) {
        String threadId = createThread(session);
        //add user message
        ThreadMessage tm = new CreateMessageRequest()
                .threadId(threadId)
                .content(prompt)
                .sendForThreadMessage();
        CreateRunRequest crr = new CreateRunRequest()
                .assistantId(assistantId)
                .instructions(assistantContext.getInstructions(session))
                .threadId(threadId);
        if (StringUtils.isNotBlank(model)) {
            crr.model(model);
        }
        return internalHandle(crr);
    }


    /**
     * 带有图片输入的对话
     *
     * @param session
     * @param prompt
     * @param imgIds  使用 uploadFile ，purpose=vision 上传图片获得的id
     * @param imgUrls 图片url  jpeg, jpg, png, gif, webp
     * @return
     */
    public String chatWithImg(String session, String prompt, List<String> imgIds, List<String> imgUrls) {
        return chatWithImg(session, prompt, imgIds, imgUrls, assistantId);
    }


    /**
     * 带有图片输入的对话
     *
     * @param session
     * @param prompt
     * @param imgIds      使用 uploadFile ，purpose=vision 上传图片获得的id
     * @param imgUrls     图片url  jpeg, jpg, png, gif, webp
     * @param assistantId
     * @return
     */
    public String chatWithImg(String session, String prompt, List<String> imgIds, List<String> imgUrls, String assistantId) {
        String threadId = createThread(session);
        //add user message
        CreateMessageRequest cmr = new CreateMessageRequest()
                .threadId(threadId)
                .addTextContent(prompt);
        if (imgIds != null && !imgIds.isEmpty()) {
            for (String imgId : imgIds) {
                cmr.addImageFileContent(imgId);
            }
        }
        if (imgUrls != null && !imgUrls.isEmpty()) {
            for (String imgUrl : imgUrls) {
                cmr.addImageURLContent(imgUrl);
            }
        }
        ThreadMessage tm = cmr.sendForThreadMessage();
        CreateRunRequest crr = new CreateRunRequest()
                .assistantId(assistantId)
                .instructions(assistantContext.getInstructions(session))
                .threadId(threadId);
        if (StringUtils.isNotBlank(model)) {
            crr.model(model);
        }
        return internalHandle(crr);
    }

    private String internalHandle(CreateRunRequest crr) {
        ThreadRun tr = crr.sendForThreadRun();
        String threadId = tr.getThread_id();
        String status = tr.getStatus();
        if (status.equals("queued") || status.equals("in_progress")) {
            System.out.println(tr);
            // wait run
            do {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                ThreadRun trs = new RetrieveRunRequest().threadId(threadId).runId(tr.getId()).sendForThreadRun();
                status = trs.getStatus();
            } while (status.equals("queued") || status.equals("in_progress"));
        }
        if (status.equals("completed")) {
            return new ListMessagesRequest().threadId(threadId).sendForMessages().get(0).getContent().get(0).getText().getString("value");
        } else if (status.equals("expired")) {
            // 如果超时了 就重试
            return internalHandle(crr);
        } else {
            return "threadId: " + threadId + "\nrunId: " + tr.getId() + "\nstatus: " + status;
        }
    }

    /**
     * 清除聊天的session
     *
     * @param session
     * @return
     */
    public String clear(String session) {
        if (assistantContext.checkThreadId(session)) {
            String threadId = assistantContext.getThreadId(session);
            try {
                new DeleteThreadRequest().threadId(threadId).sendForBool();
            } catch (Exception e) {
                // thread本身已经过期
            }
            assistantContext.clearThreadId(session);
        }
        return "操作成功";
    }

    /**
     * 获得 某一session下所有的对话
     *
     * @param session
     * @return
     */
    public List<ThreadMessage> threadMessages(String session) {
        String threadId = assistantContext.getThreadId(session);
        return new ListMessagesRequest().threadId(threadId).sendForMessages();
    }


}
