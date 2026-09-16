package com.macro.mall.admin.service;

import com.github.pagehelper.PageHelper;
import com.macro.mall.common.api.CommonPage;
import com.macro.mall.common.exception.Asserts;
import com.macro.mall.mbg.mapper.UmsResourceMapper;
import com.macro.mall.mbg.model.UmsResource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UmsResourceService {
    private final UmsResourceMapper resourceMapper;
    private final AuthorizationCacheEvictor cacheEvictor;

    public UmsResourceService(UmsResourceMapper resourceMapper, AuthorizationCacheEvictor cacheEvictor) {
        this.resourceMapper = resourceMapper;
        this.cacheEvictor = cacheEvictor;
    }

    /**
     * 资源的增删改都会改变「URL -> 所需权限」的映射，必须让权限缓存失效，
     * 否则新加的资源要等缓存过期才生效。这是接口契约的一部分。
     */
    public int create(UmsResource resource) {
        validate(resource);
        resource.setId(null);
        resource.setCreateTime(LocalDateTime.now());
        int count = resourceMapper.insert(resource);
        cacheEvictor.evict();
        return count;
    }

    public int update(Long id, UmsResource resource) {
        UmsResource existing = resourceMapper.selectById(id);
        if (existing == null) {
            Asserts.fail("资源不存在");
        }
        validate(resource);
        resource.setId(id);
        resource.setCreateTime(existing.getCreateTime());
        int count = resourceMapper.updateById(resource);
        cacheEvictor.evict();
        return count;
    }

    public UmsResource getItem(Long id) {
        return resourceMapper.selectById(id);
    }

    public int delete(Long id) {
        int count = resourceMapper.deleteById(id);
        cacheEvictor.evict();
        return count;
    }

    public CommonPage<UmsResource> list(Long categoryId, String nameKeyword, String urlKeyword,
                                        Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        return CommonPage.restPage(resourceMapper.selectList(categoryId, nameKeyword, urlKeyword));
    }

    public List<UmsResource> listAll() {
        return resourceMapper.selectAll();
    }

    private void validate(UmsResource resource) {
        if (resource.getName() == null || resource.getName().isBlank()) {
            Asserts.fail("资源名称不能为空");
        }
        if (resource.getUrl() == null || resource.getUrl().isBlank()) {
            Asserts.fail("资源URL不能为空");
        }
    }
}
