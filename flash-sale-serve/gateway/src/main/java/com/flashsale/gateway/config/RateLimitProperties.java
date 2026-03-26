package com.flashsale.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关限流配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "flash-sale.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;

    private List<Rule> rules = new ArrayList<>();

    @Data
    public static class Rule {
        private String id;
        private String path;
        private String method;
        private int maxRequests;
        private int windowSeconds;
    }
}
