package com.macro.mall.admin.controller;

import com.macro.mall.admin.service.UmsRoleService;
import com.macro.mall.admin.support.IdListParams;
import com.macro.mall.common.api.CommonPage;
import com.macro.mall.common.api.CommonResult;
import com.macro.mall.mbg.model.UmsMenu;
import com.macro.mall.mbg.model.UmsResource;
import com.macro.mall.mbg.model.UmsRole;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/role")
public class UmsRoleController {
    private static final String DEFAULT_PAGE_NUM = "1";
    private static final String DEFAULT_PAGE_SIZE = "5";

    private final UmsRoleService roleService;

    public UmsRoleController(UmsRoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping("/create")
    public CommonResult<Integer> create(@RequestBody UmsRole role) {
        return CommonResult.success(roleService.create(role));
    }

    @PostMapping("/update/{id}")
    public CommonResult<Integer> update(@PathVariable Long id, @RequestBody UmsRole role) {
        return CommonResult.success(roleService.update(id, role));
    }

    @PostMapping("/delete")
    public CommonResult<Integer> delete(@RequestParam(required = false) List<String> ids) {
        return CommonResult.success(roleService.delete(IdListParams.parse(ids)));
    }

    @GetMapping("/listAll")
    public CommonResult<List<UmsRole>> listAll() {
        return CommonResult.success(roleService.listAll());
    }

    @GetMapping("/list")
    public CommonResult<CommonPage<UmsRole>> list(@RequestParam(required = false) String keyword,
                                                  @RequestParam(defaultValue = DEFAULT_PAGE_NUM) Integer pageNum,
                                                  @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) Integer pageSize) {
        return CommonResult.success(roleService.list(keyword, pageNum, pageSize));
    }

    @PostMapping("/updateStatus/{id}")
    public CommonResult<Integer> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return CommonResult.success(roleService.updateStatus(id, status));
    }

    @GetMapping("/listMenu/{roleId}")
    public CommonResult<List<UmsMenu>> listMenu(@PathVariable Long roleId) {
        return CommonResult.success(roleService.listMenu(roleId));
    }

    @GetMapping("/listResource/{roleId}")
    public CommonResult<List<UmsResource>> listResource(@PathVariable Long roleId) {
        return CommonResult.success(roleService.listResource(roleId));
    }

    @PostMapping("/allocMenu")
    public CommonResult<Integer> allocMenu(@RequestParam Long roleId,
                                           @RequestParam(required = false) List<String> menuIds) {
        return CommonResult.success(roleService.allocMenu(roleId, IdListParams.parse(menuIds)));
    }

    @PostMapping("/allocResource")
    public CommonResult<Integer> allocResource(@RequestParam Long roleId,
                                               @RequestParam(required = false) List<String> resourceIds) {
        return CommonResult.success(roleService.allocResource(roleId, IdListParams.parse(resourceIds)));
    }
}
