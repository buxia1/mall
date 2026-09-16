package com.macro.mall.admin.support;

import com.macro.mall.common.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 前端把多选 ID 用逗号拼成一个 query 参数，一个都没选时传空串。
 * 空串必须解析成空列表，否则绑定 Long 会失败并返回 400。
 */
class IdListParamsTest {

    @Test
    void parsesACommaSeparatedValue() {
        assertThat(IdListParams.parse(List.of("1,2,3"))).containsExactly(1L, 2L, 3L);
    }

    @Test
    void parsesRepeatedParameters() {
        assertThat(IdListParams.parse(List.of("1", "2"))).containsExactly(1L, 2L);
    }

    @Test
    void treatsAnEmptySelectionAsAnEmptyList() {
        assertThat(IdListParams.parse(List.of(""))).isEmpty();
        assertThat(IdListParams.parse(List.of(" , "))).isEmpty();
        assertThat(IdListParams.parse(null)).isEmpty();
    }

    @Test
    void ignoresBlankEntriesAroundRealIds() {
        assertThat(IdListParams.parse(List.of(" 1 ,, 2 "))).containsExactly(1L, 2L);
    }

    @Test
    void rejectsNonNumericInput() {
        assertThatThrownBy(() -> IdListParams.parse(List.of("1,abc")))
                .isInstanceOf(ApiException.class)
                .hasMessage("ID 参数格式错误");
    }
}
