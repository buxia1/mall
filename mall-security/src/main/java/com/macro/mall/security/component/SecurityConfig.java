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

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter,
                                            DynamicAuthorizationManager authorizationManager) throws Exception {
        return http
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
