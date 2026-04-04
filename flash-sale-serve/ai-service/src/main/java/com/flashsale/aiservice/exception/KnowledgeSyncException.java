package com.flashsale.aiservice.exception;

public class KnowledgeSyncException extends RuntimeException {

    public KnowledgeSyncException(String message) {
        super(message);
    }

    public KnowledgeSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
