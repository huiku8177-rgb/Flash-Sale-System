package com.flashsale.productservice.exception;

import com.flashsale.common.domain.Result;
import com.flashsale.common.domain.ResultCode;
import com.flashsale.common.exception.CommonException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
/**
 * @author strive_qin
 * @version 1.0
 * @description GlobalExceptionHandler
 * @date 2026/3/20 00:00
 */


@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 处理 @RequestBody 对象参数校验失败
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError == null
                ? "参数校验失败"
                : fieldError.getDefaultMessage();

        log.warn("请求参数校验失败: {}", message);
        return failure(ResultCode.PARAM_ERROR.getCode(), message);
    }

    // 处理路径参数、请求参数等校验失败
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("参数校验失败");

        log.warn("约束校验失败: {}", message);
        return failure(ResultCode.PARAM_ERROR.getCode(), message);
    }

    // 处理 JSON 请求体格式错误
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("请求体解析失败: {}", ex.getMessage());
        return failure(ResultCode.PARAM_ERROR.getCode(), "请求体格式不正确");
    }

    // 处理业务异常
    @ExceptionHandler(CommonException.class)
    public Result<Void> handleCommonException(CommonException ex) {
        log.warn("业务异常: code={}, message={}", ex.getCode(), ex.getMessage());
        return failure(ex.getCode(), ex.getMessage());
    }

    // 兜底异常处理
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        log.error("系统异常", ex);
        return failure(ResultCode.SERVER_ERROR.getCode(), ResultCode.SERVER_ERROR.getMessage());
    }

    private Result<Void> failure(int code, String message) {
        Result<Void> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setTimestamp(LocalDateTime.now());
        return result;
    }
}
