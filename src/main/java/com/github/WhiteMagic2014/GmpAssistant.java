package com.github.WhiteMagic2014;

import com.github.WhiteMagic2014.gptApi.Assistant.Thread.CreateThreadRequest;
import com.github.WhiteMagic2014.gptApi.Assistant.Thread.DeleteThreadRequest;
import com.github.WhiteMagic2014.gptApi.Assistant.Thread.Message.CreateMessageRequest;
import com.github.WhiteMagic2014.gptApi.Assistant.Thread.Message.ListMessagesRequest;
import com.github.WhiteMagic2014.gptApi.Assistant.Thread.Run.CreateRunRequest;
import com.github.WhiteMagic2014.gptApi.Assistant.Thread.Run.RetrieveRunRequest;
import com.github.WhiteMagic2014.gptApi.Assistant.pojo.ThreadMessage;
import com.github.WhiteMagic2014.gptApi.Assistant.pojo.ThreadRun;
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

    public String chat(String session, String prompt) {
        return chat(session, prompt, assistantId, null);
    }

    public String chat(String session, String prompt, List<String> fileIds) {
        return chat(session, prompt, assistantId, fileIds);
    }

    public String chat(String session, String prompt, String assistantId, List<String> fileIds) {
        String threadId;
        if (assistantContext.checkThreadId(session)) {
            threadId = assistantContext.getThreadId(session);
        } else {
            threadId = new CreateThreadRequest().sendForGptThread().getId();
            assistantContext.setThreadId(session, threadId);
        }
        //add user message
        ThreadMessage tm = new CreateMessageRequest()
                .threadId(threadId).fileIds(fileIds)
                .content(prompt).sendForThreadMessage();
        CreateRunRequest crr = new CreateRunRequest()
                .assistantId(assistantId)
                .instructions(assistantContext.getInstructions(session))
                .threadId(threadId);
        if (StringUtils.isNotBlank(model)) {
            crr.model(model);
        }
        ThreadRun tr = crr.sendForThreadRun();
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
                System.out.println(trs);
                status = trs.getStatus();
            } while (status.equals("queued") || status.equals("in_progress"));
        }
        if (status.equals("completed")) {
            return new ListMessagesRequest().threadId(threadId).sendForMessages().get(0).getContent().get(0).getText().getString("value");
        }
        return "threadId: " + threadId + "\nrunId: " + tr.getId() + "\nstatus: " + status;
    }

    public String clear(String session) {
        String result = "操作成功";
        if (assistantContext.checkThreadId(session)) {
            String threadId = assistantContext.getThreadId(session);
            Boolean del = new DeleteThreadRequest().threadId(threadId).sendForBool();
            if (del) {
                assistantContext.clearThreadId(session);
            } else {
                result = "操作失败";
            }
        }
        return result;
    }

}
