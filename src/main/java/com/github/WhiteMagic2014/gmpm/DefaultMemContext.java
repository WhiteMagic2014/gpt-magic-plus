package com.github.WhiteMagic2014.gmpm;

import com.github.WhiteMagic2014.beans.ChatLog;
import com.github.WhiteMagic2014.beans.ChatMemory;
import com.github.WhiteMagic2014.util.Distance;
import com.github.WhiteMagic2014.util.VectorUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description: 默认基于内存的实现方式，有能力的最好用redis重写
 * @author: magic chen
 * @date: 2024/5/13 15:12
 **/
public class DefaultMemContext extends MemContext {

    private Map<String, Queue<ChatLog>> logs = new HashMap<>(); // 对话上下文
    private Map<String, String> personality = new HashMap<>(); //性格设定
    private Map<String, List<ChatMemory>> memories = new HashMap<>(); // 记忆


    public DefaultMemContext() {
        super();
    }

    public DefaultMemContext(int maxLog, int maxMemories) {
        super(maxLog, maxMemories);
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

    @Override
    public List<ChatMemory> searchMemory(String session, String prompt) {
        if (memories.containsKey(session)) {
            List<Double> q = VectorUtil.input2Vector(prompt);
            List<ChatMemory> sorted = memories.get(session).parallelStream()
                    .peek(m -> m.setEmbeddingWithQuery(Distance.cosineDistance(q, m.getEmbedding())))
                    .sorted(Comparator.comparing(ChatMemory::getEmbeddingWithQuery).reversed())
                    .limit(maxMemories)
                    .collect(Collectors.toList());
            return sorted;
        }
        return new ArrayList<>();
    }

    @Override
    public boolean addMemory(String session, ChatMemory memory) {
        if (memories.containsKey(session)) {
            List<ChatMemory> tmp = memories.get(session);
            tmp.add(memory);
        } else {
            List<ChatMemory> tmp = new ArrayList<>();
            tmp.add(memory);
            memories.put(session, tmp);
        }
        return true;
    }
}
