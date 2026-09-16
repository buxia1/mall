package com.macro.mall.security.component;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 跨域配置。
 *
 * <p>前端（mall-admin-web / mall-app-web）跑在 Vite dev server 上，直接以
 * {@code VITE_BASE_SERVER_URL=http://localhost:8080} 调后端，属于跨域请求；
 * 后端不放行的话浏览器连预检（OPTIONS）都过不去，登录根本发不出去。
 *
 * <p>默认只放开本机开发端口；部署时通过 {@code mall.security.cors.allowed-origin-patterns} 覆盖。
 */
@Component
@ConfigurationProperties(prefix = "mall.security.cors")
public class CorsProperties {
    private List<String> allowedOriginPatterns = List.of("http://localhost:*", "http://127.0.0.1:*");

    public List<String> getAllowedOriginPatterns() {
        return allowedOriginPatterns;
    }

    public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns;
    }
}
