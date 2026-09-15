package com.macro.mall.common.exception;

import com.macro.mall.common.api.IErrorCode;
import com.macro.mall.common.api.ResultCode;

public class ApiException extends RuntimeException {

    private final IErrorCode errorCode;

    public ApiException(IErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ApiException(IErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ApiException(String message) {
        this(ResultCode.FAILED, message);
    }

    public IErrorCode getErrorCode() {
        return errorCode;
    }
}
