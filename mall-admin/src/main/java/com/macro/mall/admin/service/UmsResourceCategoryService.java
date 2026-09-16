package com.macro.mall.admin.service;

import com.macro.mall.common.exception.Asserts;
import com.macro.mall.mbg.mapper.UmsResourceCategoryMapper;
import com.macro.mall.mbg.model.UmsResourceCategory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UmsResourceCategoryService {
    private final UmsResourceCategoryMapper categoryMapper;

    public UmsResourceCategoryService(UmsResourceCategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public List<UmsResourceCategory> listAll() {
        return categoryMapper.selectAll();
    }

    public int create(UmsResourceCategory category) {
        if (category.getName() == null || category.getName().isBlank()) {
            Asserts.fail("分类名称不能为空");
        }
        category.setId(null);
        category.setCreateTime(LocalDateTime.now());
        return categoryMapper.insert(category);
    }

    public int update(Long id, UmsResourceCategory category) {
        UmsResourceCategory existing = categoryMapper.selectById(id);
        if (existing == null) {
            Asserts.fail("资源分类不存在");
        }
        category.setId(id);
        // 更新语句是全字段覆盖，createTime 必须沿用库里的值，否则会被置空。
        category.setCreateTime(existing.getCreateTime());
        return categoryMapper.updateById(category);
    }

    public int delete(Long id) {
        return categoryMapper.deleteById(id);
    }
}
