package com.macro.mall.common.log;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RequestUtilTest {

    @Test
    void skipsMultipartRequestPayload() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("multipart/form-data; boundary=boundary");
        request.addParameter("file", "not-for-logs");

        assertThat(RequestUtil.getSafeParameters(request)).isEqualTo(Map.of("_logging", "[SKIPPED]"));
    }

    @Test
    void skipsLargeFormLikeRequestsBeforeReadingParameters() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/x-www-form-urlencoded");
        request.setContent(new byte[65 * 1024]);
        request.addParameter("email", "customer@example.com");

        assertThat(RequestUtil.getSafeParameters(request)).isEqualTo(Map.of("_logging", "[SKIPPED]"));
    }

    @Test
    void skipsRequestsWithTooManyParameters() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        for (int index = 0; index < 101; index++) {
            request.addParameter("parameter" + index, "value");
        }

        assertThat(RequestUtil.getSafeParameters(request)).isEqualTo(Map.of("_logging", "[SKIPPED]"));
    }

    @Test
    void redactsSensitiveParametersWhenRequestIsWithinLoggingLimits() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("password", "not-for-logs");
        request.addParameter("accessToken", "not-for-logs");
        request.addParameter("authorization", "not-for-logs");
        request.addParameter("page", "1");

        assertThat(RequestUtil.getSafeParameters(request)).containsEntry("password", "[REDACTED]")
                .containsEntry("accessToken", "[REDACTED]")
                .containsEntry("authorization", "[REDACTED]")
                .containsEntry("page", "1");
    }
}
