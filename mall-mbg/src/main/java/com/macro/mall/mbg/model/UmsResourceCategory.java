package com.macro.mall.mbg.model;

import java.time.LocalDateTime;

public class UmsResourceCategory {
    private Long id; private LocalDateTime createTime; private String name; private Integer sort;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public Integer getSort() { return sort; } public void setSort(Integer sort) { this.sort = sort; }
}
