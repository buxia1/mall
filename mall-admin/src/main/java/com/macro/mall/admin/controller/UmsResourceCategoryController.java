package com.macro.mall.admin.controller;

import com.macro.mall.admin.service.UmsResourceCategoryService;
import com.macro.mall.common.api.CommonResult;
import com.macro.mall.mbg.model.UmsResourceCategory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/resourceCategory")
public class UmsResourceCategoryController {

    private final UmsResourceCategoryService categoryService;

    public UmsResourceCategoryController(UmsResourceCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/listAll")
    public CommonResult<List<UmsResourceCategory>> listAll() {
        return CommonResult.success(categoryService.listAll());
    }

    @PostMapping("/create")
    public CommonResult<Integer> create(@RequestBody UmsResourceCategory category) {
        return CommonResult.success(categoryService.create(category));
    }

    @PostMapping("/update/{id}")
    public CommonResult<Integer> update(@PathVariable Long id, @RequestBody UmsResourceCategory category) {
        return CommonResult.success(categoryService.update(id, category));
    }

    @PostMapping("/delete/{id}")
    public CommonResult<Integer> delete(@PathVariable Long id) {
        return CommonResult.success(categoryService.delete(id));
    }
}
