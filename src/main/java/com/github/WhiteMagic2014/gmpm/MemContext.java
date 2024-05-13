package com.github.WhiteMagic2014.gmpm;

import com.github.WhiteMagic2014.beans.ChatLog;
import com.github.WhiteMagic2014.beans.ChatMemory;

import java.util.List;
import java.util.Queue;

/**
 * @Description: GmpMemory 使用的上下文记忆接口 默认提供了基于内存的实现方式，有能力的最好用redis或者其他外部存储来实现
 * @author: magic chen
 * @date: 2024/5/13 14:58
 **/
public abstract class MemContext {

    // 最大上下文层数
    protected int maxLog;

    protected int maxMemories; // 最大使用记忆层数

    public MemContext() {
        this.maxLog = 5;
        this.maxMemories = 3;
    }

    public MemContext(int maxLog, int maxMemories) {
        this.maxLog = maxLog;
        this.maxMemories = maxMemories;
    }

    /**
     * 获取 chat的上下文
     *
     * @param session
     * @return
     */
    public abstract Queue<ChatLog> chatLogs(String session);

    /**
     * 清除 chat的上下文
     *
     * @param session
     */
    public abstract void clearChatLogs(String session);


    /**
     * 增加 chat的上下文
     *
     * @param session
     * @param user
     * @param assistant
     */
    public abstract void addChatLog(String session, String user, String assistant);

    /**
     * 直接 设置chat的上下文
     *
     * @param session
     * @param chatLogs
     */
    public abstract void setChatLog(String session, Queue<ChatLog> chatLogs);

    /**
     * 设置chat 的system设定
     *
     * @param session
     * @param setting
     */
    public abstract void setPersonality(String session, String setting);

    /**
     * 获取chat 的system设定
     *
     * @param session
     */
    public abstract String getPersonality(String session);


    /**
     * 搜索与prompt相关的 记忆
     *
     * @param session
     * @param prompt
     * @return
     */
    public abstract List<ChatMemory> searchMemory(String session, String prompt);


    /**
     * 添加记忆
     *
     * @param session
     * @param memory
     * @return
     */
    public abstract boolean addMemory(String session, ChatMemory memory);

}
