package com.macro.mall.admin.controller;

import com.macro.mall.admin.dto.AdminInfo;
import com.macro.mall.admin.dto.AdminLoginParam;
import com.macro.mall.admin.dto.TokenResponse;
import com.macro.mall.admin.service.UmsAdminService;
import com.macro.mall.common.api.CommonResult;
import com.macro.mall.security.component.AdminIdentity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class UmsAdminController {
    private static final String TOKEN_HEAD = "Bearer ";

    private final UmsAdminService adminService;

    public UmsAdminController(UmsAdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/login")
    public CommonResult<TokenResponse> login(@RequestBody AdminLoginParam loginParam) {
        String token = adminService.login(loginParam.getUsername(), loginParam.getPassword());
        return token == null ? CommonResult.failed("Invalid username or password")
                : CommonResult.success(new TokenResponse(token, TOKEN_HEAD));
    }

    @GetMapping("/refreshToken")
    public CommonResult<TokenResponse> refreshToken(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = adminService.refresh(authorization);
        return token == null ? CommonResult.unauthorized(null)
                : CommonResult.success(new TokenResponse(token, TOKEN_HEAD));
    }

    @PostMapping("/logout")
    public CommonResult<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        adminService.logout(authorization);
        return CommonResult.success(null);
    }

    @GetMapping("/info")
    public CommonResult<AdminInfo> info(@AuthenticationPrincipal AdminIdentity identity) {
        if (identity == null) {
            return CommonResult.unauthorized(null);
        }
        AdminInfo info = adminService.info(identity.id());
        return info == null ? CommonResult.unauthorized(null) : CommonResult.success(info);
    }
}
