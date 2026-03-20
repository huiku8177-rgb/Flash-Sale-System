package com.flashsale.common.domain;

import lombok.Getter;
/**
 * @author strive_qin
 * @version 1.0
 * @description ResultCode
 * @date 2026/3/20 00:00
 */


@Getter
public enum ResultCode {

    SUCCESS(200, "成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已失效"),
    FORBIDDEN(403, "无权限访问"),
    SERVER_ERROR(500, "服务器内部错误"),

    STOCK_EMPTY(2001, "库存不足"),
    REPEAT_SECKILL(2002, "请勿重复秒杀"),
    BUSINESS_ERROR(2003, "业务处理失败");

    private final int code;
    private final String message;

    ResultCode(int code, String message){
        this.code = code;
        this.message = message;
    }

}
