package com.macro.mall.common.api;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommonPageTest {

    @Test
    void restPageCopiesSpringDataPageMetadataAndContent() {
        PageImpl<String> page = new PageImpl<>(List.of("A", "B"), PageRequest.of(1, 2), 5);

        CommonPage<String> result = CommonPage.restPage(page);

        assertThat(result.getPageNum()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(2);
        assertThat(result.getTotalPage()).isEqualTo(3);
        assertThat(result.getTotal()).isEqualTo(5L);
        assertThat(result.getList()).containsExactly("A", "B");
    }
}
