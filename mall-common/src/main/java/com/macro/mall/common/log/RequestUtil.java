package com.macro.mall.common.log;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class RequestUtil {

    private static final int MAX_PARAMETER_LENGTH = 1024;
    private static final long MAX_LOGGED_CONTENT_LENGTH = 64 * 1024;
    private static final int MAX_LOGGED_PARAMETER_COUNT = 100;
    private static final Map<String, Object> SKIPPED_PARAMETERS = Map.of("_logging", "[SKIPPED]");

    private RequestUtil() {
    }

    public static Optional<HttpServletRequest> currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return Optional.of(attributes.getRequest());
        }
        return Optional.empty();
    }

    public static String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",", 2)[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp == null || realIp.isBlank() ? request.getRemoteAddr() : realIp;
    }

    public static Map<String, Object> getSafeParameters(HttpServletRequest request) {
        if (isMultipart(request) || request.getContentLengthLong() > MAX_LOGGED_CONTENT_LENGTH) {
            return SKIPPED_PARAMETERS;
        }
        Map<String, String[]> requestParameters = request.getParameterMap();
        if (requestParameters.size() > MAX_LOGGED_PARAMETER_COUNT) {
            return SKIPPED_PARAMETERS;
        }
        Map<String, Object> parameters = new LinkedHashMap<>();
        requestParameters.forEach((name, values) -> parameters.put(name, sanitize(name, values)));
        return parameters;
    }

    private static boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("multipart/");
    }

    private static Object sanitize(String name, String[] values) {
        if (isSensitive(name)) {
            return "[REDACTED]";
        }
        if (values == null || values.length == 0) {
            return "";
        }
        if (values.length == 1) {
            return truncate(values[0]);
        }
        String[] safeValues = new String[values.length];
        for (int index = 0; index < values.length; index++) {
            safeValues[index] = truncate(values[index]);
        }
        return safeValues;
    }

    private static boolean isSensitive(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return normalized.contains("password") || normalized.contains("token") || normalized.contains("authorization");
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= MAX_PARAMETER_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_PARAMETER_LENGTH) + "...";
    }
}
