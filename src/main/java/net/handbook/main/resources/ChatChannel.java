package net.handbook.main.resources;

public enum ChatChannel {
    GLOBAL("g"),
    LOCAL("l"),
    WORLD("wc"),
    LFG("lfg"),
    REPLY("r");

    ChatChannel(String chatId) {
        this.chatId = chatId;
    }

    public final String chatId;
}
