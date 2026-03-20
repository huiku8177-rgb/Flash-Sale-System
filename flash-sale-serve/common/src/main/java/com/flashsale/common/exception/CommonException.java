package com.flashsale.common.exception;

import lombok.Getter;

/**
 * 通用业务异常
 *
 * @author strive_qin
 * @version 1.0
 * @description CommonException
 * @date 2026/3/20 00:00
 */
@Getter
public class CommonException extends RuntimeException{
    private int code;

    public CommonException(String message, int code) {
        super(message);
        this.code = code;
    }

    public CommonException(String message, Throwable cause, int code) {
        super(message, cause);
        this.code = code;
    }

    public CommonException(Throwable cause, int code) {
        super(cause);
        this.code = code;
    }
}
