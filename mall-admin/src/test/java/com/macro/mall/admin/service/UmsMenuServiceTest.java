package com.macro.mall.admin.service;

import com.macro.mall.mbg.mapper.UmsMenuMapper;
import com.macro.mall.mbg.model.UmsMenu;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UmsMenuServiceTest {
    private final UmsMenuMapper menuMapper = mock(UmsMenuMapper.class);
    private final UmsMenuService service = new UmsMenuService(menuMapper);

    @Test
    void treeListNestsChildrenUnderTheirParent() {
        UmsMenu root = menu(1L, 0L, "商品", 0);
        UmsMenu child = menu(2L, 1L, "商品列表", 1);
        UmsMenu grandChild = menu(3L, 2L, "新增商品", 2);
        when(menuMapper.selectAll()).thenReturn(List.of(root, child, grandChild));

        var roots = service.treeList();

        assertThat(roots).hasSize(1);
        assertThat(roots.get(0).getTitle()).isEqualTo("商品");
        assertThat(roots.get(0).getChildren()).hasSize(1);
        assertThat(roots.get(0).getChildren().get(0).getTitle()).isEqualTo("商品列表");
        assertThat(roots.get(0).getChildren().get(0).getChildren().get(0).getTitle()).isEqualTo("新增商品");
    }

    @Test
    void createDerivesTheLevelFromTheParentMenu() {
        UmsMenu parent = menu(1L, 0L, "商品", 0);
        when(menuMapper.selectById(1L)).thenReturn(parent);

        UmsMenu created = menu(null, 1L, "商品列表", null);
        service.create(created);

        assertThat(created.getLevel()).isEqualTo(1);
        verify(menuMapper).insert(any(UmsMenu.class));
    }

    @Test
    void createTreatsAZeroParentAsTheRootLevel() {
        UmsMenu created = menu(null, 0L, "首页", null);

        service.create(created);

        assertThat(created.getLevel()).isZero();
    }

    @Test
    void deleteRefusesAMenuThatStillHasChildren() {
        when(menuMapper.selectByParentId(1L)).thenReturn(List.of(menu(2L, 1L, "子菜单", 1)));

        assertThatThrownBy(() -> service.delete(1L)).hasMessage("存在子菜单，无法删除");
    }

    private UmsMenu menu(Long id, Long parentId, String title, Integer level) {
        UmsMenu menu = new UmsMenu();
        menu.setId(id);
        menu.setParentId(parentId);
        menu.setTitle(title);
        menu.setLevel(level);
        return menu;
    }
}
