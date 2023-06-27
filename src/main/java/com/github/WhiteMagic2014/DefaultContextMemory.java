package com.github.WhiteMagic2014;

import com.github.WhiteMagic2014.beans.ChatLog;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * 默认基于内存的实现方式，有能力的最好用redis重写
 */
public class DefaultContextMemory extends ContextMemory {

    private Map<String, Queue<ChatLog>> logs = new HashMap<>(); // 对话上下文
    private Map<String, String> personality = new HashMap<>(); //性格设定

    public DefaultContextMemory(int maxLog) {
        super(maxLog);
    }

    @Override
    public Queue<ChatLog> chatLogs(String session) {
        return logs.get(session);
    }

    @Override
    public void clearChatLogs(String session) {
        logs.remove(session);
    }

    @Override
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

    @Override
    public void setChatLog(String session, Queue<ChatLog> chatLogs) {
        logs.put(session, chatLogs);
    }

    @Override
    public void setPersonality(String session, String setting) {
        personality.put(session, setting);
    }

    @Override
    public String getPersonality(String session) {
        return personality.getOrDefault(session, "与用户进行闲聊或娱乐性的对话，以改善用户体验。");
    }
}
