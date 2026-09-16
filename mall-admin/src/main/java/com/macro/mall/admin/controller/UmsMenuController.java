package com.macro.mall.admin.controller;

import com.macro.mall.admin.dto.UmsMenuNode;
import com.macro.mall.admin.service.UmsMenuService;
import com.macro.mall.common.api.CommonPage;
import com.macro.mall.common.api.CommonResult;
import com.macro.mall.mbg.model.UmsMenu;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/menu")
public class UmsMenuController {
    private static final String DEFAULT_PAGE_NUM = "1";
    private static final String DEFAULT_PAGE_SIZE = "5";

    private final UmsMenuService menuService;

    public UmsMenuController(UmsMenuService menuService) {
        this.menuService = menuService;
    }

    @PostMapping("/create")
    public CommonResult<Integer> create(@RequestBody UmsMenu menu) {
        return CommonResult.success(menuService.create(menu));
    }

    @PostMapping("/update/{id}")
    public CommonResult<Integer> update(@PathVariable Long id, @RequestBody UmsMenu menu) {
        return CommonResult.success(menuService.update(id, menu));
    }

    @GetMapping("/{id}")
    public CommonResult<UmsMenu> getItem(@PathVariable Long id) {
        UmsMenu menu = menuService.getItem(id);
        return menu == null ? CommonResult.failed("菜单不存在") : CommonResult.success(menu);
    }

    @PostMapping("/delete/{id}")
    public CommonResult<Integer> delete(@PathVariable Long id) {
        return CommonResult.success(menuService.delete(id));
    }

    @GetMapping("/list/{parentId}")
    public CommonResult<CommonPage<UmsMenu>> list(@PathVariable Long parentId,
                                                  @RequestParam(defaultValue = DEFAULT_PAGE_NUM) Integer pageNum,
                                                  @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) Integer pageSize) {
        return CommonResult.success(menuService.list(parentId, pageNum, pageSize));
    }

    @GetMapping("/treeList")
    public CommonResult<List<UmsMenuNode>> treeList() {
        return CommonResult.success(menuService.treeList());
    }

    @PostMapping("/updateHidden/{id}")
    public CommonResult<Integer> updateHidden(@PathVariable Long id, @RequestParam Integer hidden) {
        return CommonResult.success(menuService.updateHidden(id, hidden));
    }
}
