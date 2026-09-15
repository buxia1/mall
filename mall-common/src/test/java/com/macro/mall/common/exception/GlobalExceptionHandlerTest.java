package com.macro.mall.common.exception;

import com.macro.mall.common.api.ResultCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setMessageInterpolator(new ParameterMessageInterpolator());
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void businessExceptionReturnsItsCodeAndMessage() throws Exception {
        mockMvc.perform(get("/business"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("库存不足"));
    }

    @Test
    void invalidBeanReturnsValidationFailureCode() throws Exception {
        mockMvc.perform(post("/quantity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("quantity must be positive"));
    }

    @Test
    void unexpectedExceptionDoesNotExposeDetails() throws Exception {
        mockMvc.perform(get("/unexpected"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value(ResultCode.FAILED.getMessage()));
    }

    @Test
    void apiExceptionUsesCustomMessageAndErrorCode() {
        ApiException exception = new ApiException(ResultCode.FAILED, "库存不足");

        assertThat(exception.getErrorCode()).isEqualTo(ResultCode.FAILED);
        assertThat(exception).hasMessage("库存不足");
    }

    @Test
    void assertsThrowsApiExceptionsWithRequestedDetails() {
        assertThatThrownBy(() -> Asserts.fail("库存不足"))
                .isInstanceOf(ApiException.class)
                .hasMessage("库存不足");
        assertThatThrownBy(() -> Asserts.fail(ResultCode.VALIDATE_FAILED))
                .isInstanceOf(ApiException.class)
                .hasMessage(ResultCode.VALIDATE_FAILED.getMessage());
        assertThatThrownBy(() -> Asserts.fail(ResultCode.FAILED, "库存不足"))
                .isInstanceOf(ApiException.class)
                .hasMessage("库存不足");
    }

    @RestController
    static class TestController {

        @PostMapping("/quantity")
        void quantity(@Valid @RequestBody QuantityRequest request) {
        }

        @GetMapping("/business")
        void business() {
            throw new ApiException(ResultCode.FAILED, "库存不足");
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("database password leaked");
        }
    }

    record QuantityRequest(@Min(value = 1, message = "quantity must be positive") int quantity) {
    }
}
