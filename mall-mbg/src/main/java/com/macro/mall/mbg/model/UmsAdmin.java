package com.macro.mall.mbg.model;

import java.time.LocalDateTime;

public class UmsAdmin {
    private Long id; private String username; private String password; private String icon; private String email; private String nickName; private String note; private LocalDateTime createTime; private LocalDateTime loginTime; private Integer status;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; } public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; } public void setPassword(String password) { this.password = password; }
    public String getIcon() { return icon; } public void setIcon(String icon) { this.icon = icon; }
    public String getEmail() { return email; } public void setEmail(String email) { this.email = email; }
    public String getNickName() { return nickName; } public void setNickName(String nickName) { this.nickName = nickName; }
    public String getNote() { return note; } public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getLoginTime() { return loginTime; } public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }
    public Integer getStatus() { return status; } public void setStatus(Integer status) { this.status = status; }
}
