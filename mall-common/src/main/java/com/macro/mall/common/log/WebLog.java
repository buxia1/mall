package com.macro.mall.common.log;

import java.util.Map;

public class WebLog {

    private String url;
    private String httpMethod;
    private String ip;
    private Map<String, Object> requestParameters;
    private Object responseValue;
    private long startTime;
    private long endTime;
    private long duration;
    private String method;
    private String description;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public Map<String, Object> getRequestParameters() { return requestParameters; }
    public void setRequestParameters(Map<String, Object> requestParameters) { this.requestParameters = requestParameters; }
    public Object getResponseValue() { return responseValue; }
    public void setResponseValue(Object responseValue) { this.responseValue = responseValue; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
