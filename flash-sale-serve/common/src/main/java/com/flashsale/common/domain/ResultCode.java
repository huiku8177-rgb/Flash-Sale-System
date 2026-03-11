package com.flashsale.common.domain;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200,"success"),
    PARAM_ERROR(400,"param error"),
    UNAUTHORIZED(401,"unauthorized"),
    FORBIDDEN(403,"forbidden"),
    SERVER_ERROR(500,"server error"),

    STOCK_EMPTY(2001,"stock empty"),
    REPEAT_SECKILL(2002,"repeat seckill");

    private final int code;
    private final String message;

    ResultCode(int code,String message){
        this.code = code;
        this.message = message;
    }

}
