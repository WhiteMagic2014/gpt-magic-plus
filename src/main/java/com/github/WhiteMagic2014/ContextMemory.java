package com.github.WhiteMagic2014;

import com.github.WhiteMagic2014.beans.ChatLog;

import java.util.Queue;

/**
 * Gmp chat 使用的上下文记忆接口 默认提供了基于内存的实现方式，有能力的最好用redis或者其他外部存储来实现
 */
public abstract class ContextMemory {

    // 最大上下文层数
    protected int maxLog;

    public ContextMemory(int maxLog) {
        this.maxLog = maxLog;
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

}
