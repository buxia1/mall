package com.macro.mall.common.exception;

import com.macro.mall.common.api.IErrorCode;

public final class Asserts {

    private Asserts() {
    }

    public static void fail(String message) {
        throw new ApiException(message);
    }

    public static void fail(IErrorCode errorCode) {
        throw new ApiException(errorCode);
    }

    public static void fail(IErrorCode errorCode, String message) {
        throw new ApiException(errorCode, message);
    }
}
