package com.macro.mall.admin.controller;

import com.macro.mall.admin.service.UmsResourceService;
import com.macro.mall.common.api.CommonPage;
import com.macro.mall.common.api.CommonResult;
import com.macro.mall.mbg.model.UmsResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/resource")
public class UmsResourceController {
    private static final String DEFAULT_PAGE_NUM = "1";
    private static final String DEFAULT_PAGE_SIZE = "5";

    private final UmsResourceService resourceService;

    public UmsResourceController(UmsResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PostMapping("/create")
    public CommonResult<Integer> create(@RequestBody UmsResource resource) {
        return CommonResult.success(resourceService.create(resource));
    }

    @PostMapping("/update/{id}")
    public CommonResult<Integer> update(@PathVariable Long id, @RequestBody UmsResource resource) {
        return CommonResult.success(resourceService.update(id, resource));
    }

    @GetMapping("/{id}")
    public CommonResult<UmsResource> getItem(@PathVariable Long id) {
        UmsResource resource = resourceService.getItem(id);
        return resource == null ? CommonResult.failed("资源不存在") : CommonResult.success(resource);
    }

    @PostMapping("/delete/{id}")
    public CommonResult<Integer> delete(@PathVariable Long id) {
        return CommonResult.success(resourceService.delete(id));
    }

    @GetMapping("/list")
    public CommonResult<CommonPage<UmsResource>> list(@RequestParam(required = false) Long categoryId,
                                                      @RequestParam(required = false) String nameKeyword,
                                                      @RequestParam(required = false) String urlKeyword,
                                                      @RequestParam(defaultValue = DEFAULT_PAGE_NUM) Integer pageNum,
                                                      @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) Integer pageSize) {
        return CommonResult.success(resourceService.list(categoryId, nameKeyword, urlKeyword, pageNum, pageSize));
    }

    @GetMapping("/listAll")
    public CommonResult<List<UmsResource>> listAll() {
        return CommonResult.success(resourceService.listAll());
    }
}
