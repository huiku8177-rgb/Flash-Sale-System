package com.flashsale.aiservice.exception;

public class ModelInvokeException extends RuntimeException {

    public ModelInvokeException(String message) {
        super(message);
    }

    public ModelInvokeException(String message, Throwable cause) {
        super(message, cause);
    }
}
