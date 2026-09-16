package com.macro.mall.mbg.model;

import java.time.LocalDateTime;

public class UmsResource {
    private Long id; private LocalDateTime createTime; private String name; private String url; private String description; private Long categoryId;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getUrl() { return url; } public void setUrl(String url) { this.url = url; }
    public String getDescription() { return description; } public void setDescription(String description) { this.description = description; }
    public Long getCategoryId() { return categoryId; } public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
}
