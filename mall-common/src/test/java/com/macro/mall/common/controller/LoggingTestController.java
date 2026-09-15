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
}
