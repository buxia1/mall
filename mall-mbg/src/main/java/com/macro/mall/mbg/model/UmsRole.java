package com.macro.mall.mbg.model;

import java.time.LocalDateTime;

public class UmsRole {
    private Long id; private String name; private String description; private Integer adminCount; private LocalDateTime createTime; private Integer status; private Integer sort;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getDescription() { return description; } public void setDescription(String description) { this.description = description; }
    public Integer getAdminCount() { return adminCount; } public void setAdminCount(Integer adminCount) { this.adminCount = adminCount; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public Integer getStatus() { return status; } public void setStatus(Integer status) { this.status = status; }
    public Integer getSort() { return sort; } public void setSort(Integer sort) { this.sort = sort; }
}
