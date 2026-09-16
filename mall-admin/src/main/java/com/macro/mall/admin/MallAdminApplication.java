package com.macro.mall.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 后台服务入口。
 *
 * <p>{@code scanBasePackages} 覆盖整个 {@code com.macro.mall}：统一返回/异常/日志在
 * {@code mall-common}，JWT 与动态授权在 {@code mall-security}，它们都不在本模块的包路径下，
 * 用默认扫描范围会漏掉。
 */
@SpringBootApplication(scanBasePackages = "com.macro.mall")
@MapperScan("com.macro.mall.mbg.mapper")
public class MallAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallAdminApplication.class, args);
    }
}
