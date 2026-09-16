package com.macro.mall.admin.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminSecurityConfigurationTest {
    @Test
    void suppliesBcryptPasswordEncoder() {
        var encoder = new AdminSecurityConfiguration().passwordEncoder();

        assertThat(encoder.matches("secret", encoder.encode("secret"))).isTrue();
    }
}
