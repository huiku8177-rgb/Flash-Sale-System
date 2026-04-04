package com.flashsale.aiservice.exception;

import com.flashsale.common.domain.Result;
import com.flashsale.common.domain.ResultCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public Result<Void> handleBadRequest(Exception ex) {
        return Result.error(ResultCode.PARAM_ERROR, ex.getMessage());
    }

    @ExceptionHandler({AiServiceException.class, KnowledgeSyncException.class, ModelInvokeException.class})
    public Result<Void> handleBusiness(RuntimeException ex) {
        log.warn("AI service business exception", ex);
        return Result.error(ResultCode.BUSINESS_ERROR, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleServerError(Exception ex) {
        log.error("Unexpected ai-service error", ex);
        return Result.error(ResultCode.SERVER_ERROR, ex.getMessage());
    }
}
