package com.flashsale.aiservice.util;

public final class ChatCacheKeys {

    private static final String PREFIX = "flashsale:ai:chat:";

    private ChatCacheKeys() {
    }

    public static String sessionHistory(String sessionId) {
        return PREFIX + "session:" + sessionId + ":history";
    }

    public static String sessionContext(String sessionId) {
        return PREFIX + "session:" + sessionId + ":context";
    }
}
