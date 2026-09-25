package com.eshop.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DeepSeek API 配置
 */
@Component
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekConfig {

    /** API Key */
    private String apiKey;

    /** 模型名称 */
    private String model;

    /** API 基础地址 */
    private String baseUrl;

    /** 超时时间（毫秒） */
    private int timeout;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }
}
