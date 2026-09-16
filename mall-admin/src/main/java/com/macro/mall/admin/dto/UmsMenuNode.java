package com.macro.mall.admin.dto;

import com.macro.mall.mbg.model.UmsMenu;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 后台菜单树节点。前端 {@code UmsMenuNode = UmsMenu & { children }}，所以这里用组合而不是继承，
 * 序列化后字段与 {@link UmsMenu} 平级。
 */
public class UmsMenuNode {
    private Long id;
    private Long parentId;
    private LocalDateTime createTime;
    private String title;
    private Integer level;
    private Integer sort;
    private String name;
    private String icon;
    private Integer hidden;
    private List<UmsMenuNode> children = new ArrayList<>();

    public static UmsMenuNode from(UmsMenu menu) {
        UmsMenuNode node = new UmsMenuNode();
        node.id = menu.getId();
        node.parentId = menu.getParentId();
        node.createTime = menu.getCreateTime();
        node.title = menu.getTitle();
        node.level = menu.getLevel();
        node.sort = menu.getSort();
        node.name = menu.getName();
        node.icon = menu.getIcon();
        node.hidden = menu.getHidden();
        return node;
    }

    public Long getId() { return id; }
    public Long getParentId() { return parentId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public String getTitle() { return title; }
    public Integer getLevel() { return level; }
    public Integer getSort() { return sort; }
    public String getName() { return name; }
    public String getIcon() { return icon; }
    public Integer getHidden() { return hidden; }
    public List<UmsMenuNode> getChildren() { return children; }
}
