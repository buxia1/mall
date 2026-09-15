package com.macro.mall.common.exception;

import com.macro.mall.common.api.CommonResult;
import com.macro.mall.common.api.ResultCode;
import jakarta.validation.ConstraintViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public CommonResult<Object> handleApiException(ApiException exception) {
        return CommonResult.failed(exception.getErrorCode(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public CommonResult<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        BindingResult bindingResult = exception.getBindingResult();
        String message = bindingResult.getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(messageValue -> messageValue != null && !messageValue.isBlank())
                .findFirst()
                .orElse(ResultCode.VALIDATE_FAILED.getMessage());
        return CommonResult.validateFailed(message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public CommonResult<Object> handleConstraintViolationException(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .filter(messageValue -> messageValue != null && !messageValue.isBlank())
                .findFirst()
                .orElse(ResultCode.VALIDATE_FAILED.getMessage());
        return CommonResult.validateFailed(message);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public CommonResult<Object> handleAccessDeniedException(AccessDeniedException exception) {
        return CommonResult.forbidden(null);
    }

    @ExceptionHandler(Exception.class)
    public CommonResult<Object> handleException(Exception exception) {
        return CommonResult.failed();
    }
}
