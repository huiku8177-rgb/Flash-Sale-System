package com.flashsale.orderservice.exception;

import com.flashsale.common.domain.Result;
import com.flashsale.common.domain.ResultCode;
import com.flashsale.common.exception.CommonException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError == null ? "参数校验失败" : fieldError.getDefaultMessage();
        log.warn("order validation failed: {}", message);
        return failure(ResultCode.PARAM_ERROR.getCode(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("参数校验失败");
        log.warn("order constraint violation: {}", message);
        return failure(ResultCode.PARAM_ERROR.getCode(), message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("order request body unreadable: {}", ex.getMessage());
        return failure(ResultCode.PARAM_ERROR.getCode(), "请求体格式不正确");
    }

    @ExceptionHandler(CommonException.class)
    public Result<Void> handleCommonException(CommonException ex) {
        log.warn("order business exception: code={}, message={}", ex.getCode(), ex.getMessage());
        return failure(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        log.error("order service unhandled exception", ex);
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
