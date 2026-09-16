package com.macro.mall.admin.controller;

import com.macro.mall.admin.dto.AdminInfo;
import com.macro.mall.admin.dto.AdminLoginParam;
import com.macro.mall.admin.dto.AdminPasswordParam;
import com.macro.mall.admin.dto.TokenResponse;
import com.macro.mall.admin.service.UmsAdminService;
import com.macro.mall.admin.support.IdListParams;
import com.macro.mall.common.api.CommonPage;
import com.macro.mall.common.api.CommonResult;
import com.macro.mall.mbg.model.UmsAdmin;
import com.macro.mall.mbg.model.UmsRole;
import com.macro.mall.security.component.AdminIdentity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class UmsAdminController {
    private static final String TOKEN_HEAD = "Bearer ";
    private static final String DEFAULT_PAGE_NUM = "1";
    private static final String DEFAULT_PAGE_SIZE = "5";

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

    @PostMapping("/register")
    public CommonResult<UmsAdmin> register(@RequestBody UmsAdmin admin) {
        return CommonResult.success(adminService.register(admin));
    }

    @GetMapping("/list")
    public CommonResult<CommonPage<UmsAdmin>> list(@RequestParam(required = false) String keyword,
                                                   @RequestParam(defaultValue = DEFAULT_PAGE_NUM) Integer pageNum,
                                                   @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) Integer pageSize) {
        return CommonResult.success(adminService.list(keyword, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public CommonResult<UmsAdmin> getItem(@PathVariable Long id) {
        UmsAdmin admin = adminService.getItem(id);
        return admin == null ? CommonResult.failed("用户不存在") : CommonResult.success(admin);
    }

    @PostMapping("/update/{id}")
    public CommonResult<Integer> update(@PathVariable Long id, @RequestBody UmsAdmin admin) {
        return CommonResult.success(adminService.update(id, admin));
    }

    @PostMapping("/updatePassword")
    public CommonResult<Integer> updatePassword(@RequestBody AdminPasswordParam passwordParam) {
        return CommonResult.success(adminService.updatePassword(passwordParam));
    }

    @PostMapping("/delete/{id}")
    public CommonResult<Integer> delete(@PathVariable Long id) {
        return CommonResult.success(adminService.delete(id));
    }

    @PostMapping("/updateStatus/{id}")
    public CommonResult<Integer> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return CommonResult.success(adminService.updateStatus(id, status));
    }

    @PostMapping("/role/update")
    public CommonResult<Integer> updateRole(@RequestParam Long adminId,
                                            @RequestParam(required = false) List<String> roleIds) {
        return CommonResult.success(adminService.updateRole(adminId, IdListParams.parse(roleIds)));
    }

    @GetMapping("/role/{adminId}")
    public CommonResult<List<UmsRole>> getRoleList(@PathVariable Long adminId) {
        return CommonResult.success(adminService.getRoleList(adminId));
    }
}
