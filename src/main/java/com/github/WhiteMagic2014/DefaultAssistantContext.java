package com.github.WhiteMagic2014;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description: 默认基于内存的实现方式，有能力的最好用redis重写
 * @author: magic chen
 * @date: 2023/11/21 14:55
 **/
public class DefaultAssistantContext extends AssistantContext {

    private Map<String, String> sessionThread = new HashMap<>();
    private Map<String, String> sessionInstructions = new HashMap<>();


    @Override
    public String setInstructions(String session, String instructions) {
        return sessionInstructions.put(session, instructions);
    }


    @Override
    public String getInstructions(String session) {
        return sessionInstructions.get(session);
    }


    @Override
    public String setThreadId(String session, String threadId) {
        return sessionThread.put(session, threadId);
    }

    @Override
    public String getThreadId(String session) {
        return sessionThread.get(session);
    }

    @Override
    public void clearThreadId(String session) {
        sessionThread.remove(session);
    }

    @Override
    public boolean checkThreadId(String session) {
        return sessionThread.containsKey(session);
    }
}
