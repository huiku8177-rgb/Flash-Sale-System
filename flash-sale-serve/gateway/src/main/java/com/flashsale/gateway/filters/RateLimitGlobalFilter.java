package com.flashsale.gateway.filters;

import cn.hutool.core.text.AntPathMatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.domain.ResultCode;
import com.flashsale.gateway.config.RateLimitProperties;
import com.flashsale.gateway.support.GatewayResponseWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 对敏感入口做轻量级限流。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitGlobalFilter implements GlobalFilter, Ordered {

    private static final int CLEANUP_INTERVAL = 200;

    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!rateLimitProperties.isEnabled()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();
        if (method == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        RateLimitProperties.Rule matchedRule = findMatchedRule(method, request.getPath().value());
        if (matchedRule == null) {
            return chain.filter(exchange);
        }

        cleanupExpiredCounters();

        long now = Instant.now().toEpochMilli();
        long windowMillis = Math.max(matchedRule.getWindowSeconds(), 1) * 1000L;
        String clientKey = resolveClientKey(request);
        String counterKey = matchedRule.getId() + ":" + clientKey;

        WindowCounter counter = counters.compute(counterKey, (key, current) -> {
            if (current == null || current.expiresAt <= now) {
                return new WindowCounter(now + windowMillis, 1);
            }
            current.count.incrementAndGet();
            return current;
        });

        int currentCount = counter.count.get();
        long retryAfterSeconds = Math.max(1, (counter.expiresAt - now + 999) / 1000);

        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().set("X-RateLimit-Limit", String.valueOf(matchedRule.getMaxRequests()));
        response.getHeaders().set("X-RateLimit-Remaining", String.valueOf(Math.max(0, matchedRule.getMaxRequests() - currentCount)));
        response.getHeaders().set("X-RateLimit-Reset", String.valueOf(counter.expiresAt / 1000));

        if (currentCount > matchedRule.getMaxRequests()) {
            response.getHeaders().set("Retry-After", String.valueOf(retryAfterSeconds));
            log.warn("gateway rate limited: rule={}, clientKey={}, method={}, path={}",
                    matchedRule.getId(),
                    clientKey,
                    method,
                    request.getPath().value());
            return GatewayResponseWriter.writeError(
                    response,
                    objectMapper,
                    HttpStatus.TOO_MANY_REQUESTS,
                    ResultCode.TOO_MANY_REQUESTS,
                    "请求过于频繁，请稍后再试"
            );
        }

        return chain.filter(exchange);
    }

    private RateLimitProperties.Rule findMatchedRule(HttpMethod method, String path) {
        List<RateLimitProperties.Rule> rules = rateLimitProperties.getRules();
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        for (RateLimitProperties.Rule rule : rules) {
            if (rule == null || rule.getPath() == null || rule.getPath().isBlank()) {
                continue;
            }
            boolean pathMatched = antPathMatcher.match(rule.getPath().trim(), path);
            boolean methodMatched = rule.getMethod() == null
                    || rule.getMethod().isBlank()
                    || (method != null && method.name().equals(rule.getMethod().trim().toUpperCase(Locale.ROOT)));
            boolean validLimit = rule.getMaxRequests() > 0 && rule.getWindowSeconds() > 0;
            if (pathMatched && methodMatched && validLimit) {
                if (rule.getId() == null || rule.getId().isBlank()) {
                    rule.setId((rule.getMethod() == null ? "ANY" : rule.getMethod()) + ":" + rule.getPath());
                }
                return rule;
            }
        }
        return null;
    }

    private void cleanupExpiredCounters() {
        if (requestCounter.incrementAndGet() % CLEANUP_INTERVAL != 0) {
            return;
        }
        long now = Instant.now().toEpochMilli();
        counters.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
    }

    private String resolveClientKey(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int commaIndex = forwardedFor.indexOf(',');
            return commaIndex >= 0 ? forwardedFor.substring(0, commaIndex).trim() : forwardedFor.trim();
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return "unknown";
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private static final class WindowCounter {
        private final long expiresAt;
        private final AtomicInteger count;

        private WindowCounter(long expiresAt, int initialCount) {
            this.expiresAt = expiresAt;
            this.count = new AtomicInteger(initialCount);
        }
    }
}
