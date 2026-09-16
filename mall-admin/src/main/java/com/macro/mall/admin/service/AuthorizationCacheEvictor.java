package com.macro.mall.admin.service;

import com.macro.mall.security.component.DynamicAuthorizationManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 权限缓存的失效入口。
 *
 * <p>改动 {@code ums_resource} 或角色-资源关系后必须让 {@link DynamicAuthorizationManager} 的缓存失效，
 * 这是接口契约的一部分（改了权限不重启就要生效）。
 *
 * <p>如果当前在事务里，失效动作注册到<b>提交之后</b>执行：在提交前清缓存的话，并发请求会读到未提交的旧数据
 * 并把它重新写进缓存，等于白清。
 */
@Component
public class AuthorizationCacheEvictor {
    private final DynamicAuthorizationManager authorizationManager;

    public AuthorizationCacheEvictor(DynamicAuthorizationManager authorizationManager) {
        this.authorizationManager = authorizationManager;
    }

    public void evict() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    authorizationManager.clearResourceCache();
                }
            });
        } else {
            authorizationManager.clearResourceCache();
        }
    }
}
