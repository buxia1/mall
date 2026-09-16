package com.macro.mall.security.component;

import com.macro.mall.common.exception.JsonAccessDeniedHandler;
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
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final DynamicAuthorizationManager authorizationManager;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          DynamicAuthorizationManager authorizationManager) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authorizationManager = authorizationManager;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
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
