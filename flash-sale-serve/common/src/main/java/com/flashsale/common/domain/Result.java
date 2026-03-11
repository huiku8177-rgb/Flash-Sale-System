package com.flashsale.common.domain;

import lombok.Data;

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

    public static <T> Result<T> success(T data){

        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        result.setData(data);

        return result;
    }

    public static Result error(ResultCode code){

        Result result = new Result();
        result.setCode(code.getCode());
        result.setMessage(code.getMessage());

        return result;
    }
}
