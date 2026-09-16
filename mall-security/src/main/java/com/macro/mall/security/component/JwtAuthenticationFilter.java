package com.macro.mall.security.component;

import com.macro.mall.security.service.AdminTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenUtil jwtTokenUtil;
    private final AdminTokenService tokenService;

    public JwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil, AdminTokenService tokenService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = bearerToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwtTokenUtil.getIdentity(token)
                    .filter(identity -> tokenService.isActive(token))
                    .ifPresent(identity -> authenticate(identity, request));
        }
        filterChain.doFilter(request, response);
    }

    private String bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith(BEARER_PREFIX) && header.length() > BEARER_PREFIX.length()
                ? header.substring(BEARER_PREFIX.length()) : null;
    }

    private void authenticate(AdminIdentity identity, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                identity, null, List.of());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
