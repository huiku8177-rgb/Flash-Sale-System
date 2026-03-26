package com.flashsale.authservice.exception;

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
        String message = fieldError == null ? "参数校验失败" : fieldError.getField() + " " + fieldError.getDefaultMessage();
        log.warn("auth validation failed: {}", message);
        return Result.error(ResultCode.PARAM_ERROR, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .orElse("参数校验失败");
        log.warn("auth constraint violation: {}", message);
        return Result.error(ResultCode.PARAM_ERROR, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("auth request body unreadable: {}", ex.getMessage());
        return Result.error(ResultCode.PARAM_ERROR, "请求体格式不正确");
    }

    @ExceptionHandler(CommonException.class)
    public Result<Void> handleCommonException(CommonException ex) {
        log.warn("auth business exception: code={}, message={}", ex.getCode(), ex.getMessage());
        return failure(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        log.error("auth service unhandled exception", ex);
        return Result.error(ResultCode.SERVER_ERROR);
    }

    private Result<Void> failure(int code, String message) {
        Result<Void> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        result.setTimestamp(LocalDateTime.now());
        return result;
    }
}
