package com.macro.mall.admin.support;

import com.macro.mall.common.exception.Asserts;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 解析前端传来的「逗号拼接 ID 列表」。
 *
 * <p>前端统一用 {@code Array.from(set).join(',')} 拼 query 参数（见 mall-admin-web 的 allocMenu /
 * allocResource / roleDeleteByIds / adminRoleUpdate）。一个都没选时传的是空串，
 * 直接绑定成 {@code List<Long>} 会因 "" 转 Long 失败而返回 400，所以这里统一转成 List 并过滤空值。
 */
public final class IdListParams {

    private IdListParams() {
    }

    public static List<Long> parse(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        try {
            return raw.stream()
                    .filter(Objects::nonNull)
                    .flatMap(value -> Arrays.stream(value.split(",")))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(Long::valueOf)
                    .toList();
        } catch (NumberFormatException exception) {
            Asserts.fail("ID 参数格式错误");
            return List.of();
        }
    }
}
