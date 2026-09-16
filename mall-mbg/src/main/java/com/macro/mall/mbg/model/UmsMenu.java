package com.macro.mall.mbg.model;

import java.time.LocalDateTime;

public class UmsMenu {
    private Long id; private Long parentId; private LocalDateTime createTime; private String title; private Integer level; private Integer sort; private String name; private String icon; private Integer hidden;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getParentId() { return parentId; } public void setParentId(Long parentId) { this.parentId = parentId; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getTitle() { return title; } public void setTitle(String title) { this.title = title; }
    public Integer getLevel() { return level; } public void setLevel(Integer level) { this.level = level; }
    public Integer getSort() { return sort; } public void setSort(Integer sort) { this.sort = sort; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getIcon() { return icon; } public void setIcon(String icon) { this.icon = icon; }
    public Integer getHidden() { return hidden; } public void setHidden(Integer hidden) { this.hidden = hidden; }
}
