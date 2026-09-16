package com.macro.mall.common.controller;

import com.macro.mall.common.api.CommonResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoggingTestController {

    @GetMapping("/test/success")
    public CommonResult<String> success() {
        return CommonResult.success("ok");
    }

    @GetMapping("/test/sensitive")
    public SensitiveResponse sensitive() {
        return new SensitiveResponse();
    }

    @GetMapping("/test/failure")
    public String failure() {
        throw new IllegalStateException("controller failure");
    }

    public static class SensitiveResponse {

        public String getValue() {
            return "safe";
        }

        @Override
        public String toString() {
            return "password=secret";
        }
    }
}
