package com.flashsale.common.domain;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author strive_qin
 * @version 1.0
 * @description Result
 * @date 2026/3/11 11:31
 */
@Data
public class Result<T> {

    private Integer code;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data){

        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        result.setData(data);
        result.setTimestamp(LocalDateTime.now());

        return result;
    }

    public static <T> Result<T> error(ResultCode code) {
        Result<T> result = new Result<>();
        result.setCode(code.getCode());
        result.setMessage(code.getMessage());
        return result;
    }

    public static Result<Void> error(ResultCode code, String message){

        Result<Void> result = new Result<>();
        result.setCode(code.getCode());
        result.setMessage(message);
        result.setTimestamp(LocalDateTime.now());

        return result;
    }
}
