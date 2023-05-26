package io.github.WhiteMagic2014.beans;

public class GptMessage {

    String role;//system,user,assistant

    String prompt;

    public static GptMessage systemMessage(String prompt) {
        return new GptMessage("system", prompt);
    }

    public static GptMessage userMessage(String prompt) {
        return new GptMessage("user", prompt);
    }

    public static GptMessage assistantMessage(String prompt) {
        return new GptMessage("assistant", prompt);
    }


    private GptMessage(String role, String prompt) {
        this.role = role;
        this.prompt = prompt;
    }


    public GptMessage() {
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

}
