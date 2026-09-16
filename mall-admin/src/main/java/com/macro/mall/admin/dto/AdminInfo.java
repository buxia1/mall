package com.macro.mall.admin.dto;

import com.macro.mall.mbg.model.UmsMenu;

import java.util.List;

public record AdminInfo(String username, List<UmsMenu> menus, String icon, List<String> roles) {
}
