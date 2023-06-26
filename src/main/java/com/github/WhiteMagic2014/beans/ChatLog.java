package com.github.WhiteMagic2014.beans;

import java.io.Serializable;

public class ChatLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private String user;
    private String assistant;

    public ChatLog(String user, String assistant) {
        this.user = user;
        this.assistant = assistant;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getAssistant() {
        return assistant;
    }

    public void setAssistant(String assistant) {
        this.assistant = assistant;
    }
}
