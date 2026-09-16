package com.macro.mall.security.component;

import com.macro.mall.common.exception.JsonAccessDeniedHandler;
import com.macro.mall.common.service.RedisService;
import com.macro.mall.mbg.mapper.UmsAdminMapper;
import com.macro.mall.security.service.AdminTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil,
                                                    AdminTokenService tokenService) {
        return new JwtAuthenticationFilter(jwtTokenUtil, tokenService);
    }

    @Bean
    DynamicAuthorizationManager dynamicAuthorizationManager(UmsAdminMapper adminMapper,
                                                            RedisService redisService) {
        return new DynamicAuthorizationManager(adminMapper, redisService);
    }

    /**
     * 走 Spring Security 的 {@code cors()} 而不是自己加 Filter：它会把 CorsFilter 放在鉴权之前，
     * 让 OPTIONS 预检被直接应答，不会落到 DynamicAuthorizationManager 上被判 403。
     *
     * <p>不使用 Cookie 会话（token 放在 Authorization 头），所以不开 allowCredentials。
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(corsProperties.getAllowedOriginPatterns());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter,
                                            DynamicAuthorizationManager authorizationManager,
                                            CorsConfigurationSource corsConfigurationSource) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new JsonAuthenticationEntryPoint())
                        .accessDeniedHandler(new JsonAccessDeniedHandler()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/admin/login", "/admin/refreshToken").permitAll()
                        .anyRequest().access(authorizationManager))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
