package com.flashsale.common.exception;

/**
 * 未授权异常
 *
 * @author strive_qin
 * @version 1.0
 * @description UnauthorizedException
 * @date 2026/3/20 00:00
 */
public class UnauthorizedException extends CommonException{

    public UnauthorizedException(String message) {
        super(message, 401);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause, 401);
    }

    public UnauthorizedException(Throwable cause) {
        super(cause, 401);
    }
}
