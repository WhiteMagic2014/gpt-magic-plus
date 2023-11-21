package com.github.WhiteMagic2014;

/**
 * @Description: TODO
 * @author: magic chen
 * @date: 2023/11/21 14:51
 **/
public abstract class AssistantContext {

    /**
     * 设置用户的Instructions
     *
     * @param session
     * @return
     */
    public abstract String setInstructions(String session, String instructions);

    /**
     * 获得用户的Instructions
     *
     * @param session
     * @return
     */
    public abstract String getInstructions(String session);


    /**
     * 绑定 对话和 thread
     *
     * @param session
     * @param threadId
     * @return
     */
    public abstract String setThreadId(String session, String threadId);

    /**
     * 获得对话的 threadId
     *
     * @param session
     * @return
     */
    public abstract String getThreadId(String session);

    /**
     * 清除对话的 thread
     *
     * @param session
     */
    public abstract void clearThreadId(String session);

    /**
     * 检查对话是否有 thread
     *
     * @param session
     * @return
     */
    public abstract boolean checkThreadId(String session);
}
