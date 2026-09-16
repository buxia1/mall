package com.macro.mall.admin.service;

import com.github.pagehelper.PageHelper;
import com.macro.mall.admin.dto.UmsMenuNode;
import com.macro.mall.common.api.CommonPage;
import com.macro.mall.common.exception.Asserts;
import com.macro.mall.mbg.mapper.UmsMenuMapper;
import com.macro.mall.mbg.model.UmsMenu;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UmsMenuService {
    private static final long ROOT_PARENT_ID = 0L;

    private final UmsMenuMapper menuMapper;

    public UmsMenuService(UmsMenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    public int create(UmsMenu menu) {
        if (menu.getTitle() == null || menu.getTitle().isBlank()) {
            Asserts.fail("菜单名称不能为空");
        }
        menu.setId(null);
        menu.setCreateTime(LocalDateTime.now());
        menu.setLevel(resolveLevel(menu.getParentId()));
        if (menu.getSort() == null) {
            menu.setSort(0);
        }
        if (menu.getHidden() == null) {
            menu.setHidden(0);
        }
        return menuMapper.insert(menu);
    }

    public int update(Long id, UmsMenu menu) {
        UmsMenu existing = menuMapper.selectById(id);
        if (existing == null) {
            Asserts.fail("菜单不存在");
        }
        menu.setId(id);
        menu.setCreateTime(existing.getCreateTime());
        menu.setLevel(resolveLevel(menu.getParentId()));
        return menuMapper.updateById(menu);
    }

    public UmsMenu getItem(Long id) {
        return menuMapper.selectById(id);
    }

    public int delete(Long id) {
        if (!menuMapper.selectByParentId(id).isEmpty()) {
            Asserts.fail("存在子菜单，无法删除");
        }
        return menuMapper.deleteById(id);
    }

    public CommonPage<UmsMenu> list(Long parentId, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        return CommonPage.restPage(menuMapper.selectByParentId(parentId));
    }

    /** 一次性取回全部菜单后在内存组树，避免按层级递归查库。 */
    public List<UmsMenuNode> treeList() {
        Map<Long, UmsMenuNode> nodes = new LinkedHashMap<>();
        for (UmsMenu menu : menuMapper.selectAll()) {
            nodes.put(menu.getId(), UmsMenuNode.from(menu));
        }
        List<UmsMenuNode> roots = new ArrayList<>();
        for (UmsMenuNode node : nodes.values()) {
            UmsMenuNode parent = node.getParentId() == null ? null : nodes.get(node.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    public int updateHidden(Long id, Integer hidden) {
        return menuMapper.updateHidden(id, hidden);
    }

    private int resolveLevel(Long parentId) {
        if (parentId == null || parentId == ROOT_PARENT_ID) {
            return 0;
        }
        UmsMenu parent = menuMapper.selectById(parentId);
        if (parent == null) {
            Asserts.fail("父级菜单不存在");
        }
        return parent.getLevel() == null ? 1 : parent.getLevel() + 1;
    }
}
