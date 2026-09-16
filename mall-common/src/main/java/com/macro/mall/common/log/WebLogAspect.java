package com.macro.mall.common.log;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class WebLogAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebLogAspect.class);

    @Around("execution(public * com.macro.mall..controller..*(..))")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        WebLog webLog = new WebLog();
        webLog.setStartTime(System.currentTimeMillis());
        webLog.setMethod(point.getSignature().toLongString());
        webLog.setResponseValue("[not-completed]");
        RequestUtil.currentRequest().ifPresent(request -> fillRequest(webLog, request));
        try {
            Object response = point.proceed();
            webLog.setResponseValue(safeResponseSummary(response));
            return response;
        } finally {
            long ended = System.currentTimeMillis();
            webLog.setEndTime(ended);
            webLog.setDuration(ended - webLog.getStartTime());
            LOGGER.info("web request: url={}, httpMethod={}, ip={}, parameters={}, response={}, method={}, durationMs={}",
                    webLog.getUrl(), webLog.getHttpMethod(), webLog.getIp(), webLog.getRequestParameters(),
                    webLog.getResponseValue(), webLog.getMethod(), webLog.getDuration());
        }
    }

    private void fillRequest(WebLog webLog, HttpServletRequest request) {
        webLog.setUrl(request.getRequestURL().toString());
        webLog.setHttpMethod(request.getMethod());
        webLog.setIp(RequestUtil.getClientIp(request));
        webLog.setRequestParameters(RequestUtil.getSafeParameters(request));
    }

    private String safeResponseSummary(Object response) {
        return response == null ? "[not-completed]" : response.getClass().getSimpleName() + " [omitted]";
    }
}
