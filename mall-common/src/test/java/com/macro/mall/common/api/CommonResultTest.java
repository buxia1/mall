package com.macro.mall.common.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommonResultTest {

    @Test
    void successUsesSuccessCodeAndDefaultMessage() {
        CommonResult<String> result = CommonResult.success("ok");

        assertThat(result.getCode()).isEqualTo(200L);
        assertThat(result.getMessage()).isEqualTo("操作成功");
        assertThat(result.getData()).isEqualTo("ok");
    }

    @Test
    void successCanUseAnOverrideMessage() {
        CommonResult<String> result = CommonResult.success("ok", "created");

        assertThat(result.getCode()).isEqualTo(200L);
        assertThat(result.getMessage()).isEqualTo("created");
        assertThat(result.getData()).isEqualTo("ok");
    }

    @Test
    void failedErrorCodeCanUseAnOverrideMessage() {
        CommonResult<Object> result = CommonResult.failed(ResultCode.FAILED, "库存不足");

        assertThat(result.getCode()).isEqualTo(500L);
        assertThat(result.getMessage()).isEqualTo("库存不足");
        assertThat(result.getData()).isNull();
    }

    @Test
    void specializedFactoriesUseTheirSemanticCodes() {
        assertThat(CommonResult.validateFailed()).extracting(CommonResult::getCode, CommonResult::getMessage)
                .containsExactly(404L, "参数检验失败");
        assertThat(CommonResult.validateFailed("invalid")).extracting(CommonResult::getCode, CommonResult::getMessage)
                .containsExactly(404L, "invalid");
        assertThat(CommonResult.unauthorized("token")).extracting(CommonResult::getCode, CommonResult::getData)
                .containsExactly(401L, "token");
        assertThat(CommonResult.forbidden("permission")).extracting(CommonResult::getCode, CommonResult::getData)
                .containsExactly(403L, "permission");
    }

    @Test
    void failedFactoriesUseTheFailedCode() {
        assertThat(CommonResult.failed()).extracting(CommonResult::getCode, CommonResult::getMessage)
                .containsExactly(500L, "操作失败");
        assertThat(CommonResult.failed("unavailable")).extracting(CommonResult::getCode, CommonResult::getMessage)
                .containsExactly(500L, "unavailable");
    }

    @Test
    void resultCanBeConstructedAndUpdatedWithinTheApiPackage() {
        CommonResult<String> result = new CommonResult<>(1L, "initial", "data");

        result.setCode(2L);
        result.setMessage("updated");
        result.setData("replacement");

        assertThat(result).extracting(CommonResult::getCode, CommonResult::getMessage, CommonResult::getData)
                .containsExactly(2L, "updated", "replacement");
    }
}
