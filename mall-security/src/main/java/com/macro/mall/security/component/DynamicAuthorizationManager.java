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
        String requestPath = applicationPath(context);
        boolean granted = resourceUrlsFor(identity.id()).stream()
                .anyMatch(resourceUrl -> pathMatcher.match(resourceUrl, requestPath));
        return new AuthorizationDecision(granted);
    }

    public void clearResourceCache() {
        redisService.remove(RESOURCE_CACHE_KEY);
    }

    private String applicationPath(RequestAuthorizationContext context) {
        String servletPath = context.getRequest().getServletPath();
        if (servletPath != null && !servletPath.isEmpty()) {
            return servletPath;
        }
        String contextPath = context.getRequest().getContextPath();
        String requestUri = context.getRequest().getRequestURI();
        return requestUri.startsWith(contextPath) ? requestUri.substring(contextPath.length()) : requestUri;
    }

    private Set<String> resourceUrlsFor(Long adminId) {
        Map<String, Set<String>> cache = resourceCache();
        String cacheKey = String.valueOf(adminId);
        Set<String> resourceUrls = cache.get(cacheKey);
        if (resourceUrls != null) {
            return resourceUrls;
        }
        Set<String> loadedUrls = resourceUrls(adminMapper.selectResourcesByAdminId(adminId));
        cache.put(cacheKey, loadedUrls);
        redisService.set(RESOURCE_CACHE_KEY, cache);
        return loadedUrls;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Set<String>> resourceCache() {
        Object cached = redisService.get(RESOURCE_CACHE_KEY);
        if (!(cached instanceof Map<?, ?> cachedMap)) {
            return new HashMap<>();
        }
        Map<String, Set<String>> cache = new HashMap<>();
        cachedMap.forEach((adminId, urls) -> {
            if (adminId instanceof String id && urls instanceof Collection<?> resourceUrls) {
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
