package com.macro.mall.security.component;

import com.macro.mall.common.service.RedisService;
import com.macro.mall.mbg.mapper.UmsAdminMapper;
import com.macro.mall.mbg.model.UmsResource;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.util.AntPathMatcher;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class DynamicAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
    private static final String RESOURCE_CACHE_KEY = "mall:security:authorization:resources";

    private final UmsAdminMapper adminMapper;
    private final RedisService redisService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public DynamicAuthorizationManager(UmsAdminMapper adminMapper, RedisService redisService) {
        this.adminMapper = adminMapper;
        this.redisService = redisService;
    }

    @Override
    @SuppressWarnings("deprecation")
    public AuthorizationDecision check(Supplier<Authentication> authentication,
                                       RequestAuthorizationContext context) {
        Authentication currentAuthentication = authentication.get();
        if (currentAuthentication == null || !currentAuthentication.isAuthenticated()
                || !(currentAuthentication.getPrincipal() instanceof AdminIdentity identity)) {
            return new AuthorizationDecision(false);
        }
        String requestPath = context.getRequest().getRequestURI();
        boolean granted = resourceUrlsFor(identity.id()).stream()
                .anyMatch(resourceUrl -> pathMatcher.match(resourceUrl, requestPath));
        return new AuthorizationDecision(granted);
    }

    public void clearResourceCache() {
        redisService.remove(RESOURCE_CACHE_KEY);
    }

    private Set<String> resourceUrlsFor(Long adminId) {
        Map<Long, Set<String>> cache = resourceCache();
        Set<String> resourceUrls = cache.get(adminId);
        if (resourceUrls != null) {
            return resourceUrls;
        }
        Set<String> loadedUrls = resourceUrls(adminMapper.selectResourcesByAdminId(adminId));
        cache.put(adminId, loadedUrls);
        redisService.set(RESOURCE_CACHE_KEY, cache);
        return loadedUrls;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Set<String>> resourceCache() {
        Object cached = redisService.get(RESOURCE_CACHE_KEY);
        if (!(cached instanceof Map<?, ?> cachedMap)) {
            return new HashMap<>();
        }
        Map<Long, Set<String>> cache = new HashMap<>();
        cachedMap.forEach((adminId, urls) -> {
            if (adminId instanceof Long id && urls instanceof Collection<?> resourceUrls) {
                cache.put(id, resourceUrls.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .collect(java.util.stream.Collectors.toSet()));
            }
        });
        return cache;
    }

    private Set<String> resourceUrls(Collection<UmsResource> resources) {
        Set<String> urls = new HashSet<>();
        if (resources != null) {
            resources.stream().map(UmsResource::getUrl).filter(url -> url != null && !url.isBlank())
                    .forEach(urls::add);
        }
        return urls;
    }
}
