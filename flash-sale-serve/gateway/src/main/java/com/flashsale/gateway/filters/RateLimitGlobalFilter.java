package com.flashsale.gateway.filters;

import cn.hutool.core.text.AntPathMatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.domain.ResultCode;
import com.flashsale.common.redis.RedisKeys;
import com.flashsale.gateway.config.RateLimitProperties;
import com.flashsale.gateway.support.GatewayResponseWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitGlobalFilter implements GlobalFilter, Ordered {

    private final RateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;
    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

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

        long nowEpochSeconds = Instant.now().getEpochSecond();
        long windowSeconds = Math.max(matchedRule.getWindowSeconds(), 1L);
        long windowStartEpochSeconds = (nowEpochSeconds / windowSeconds) * windowSeconds;
        long resetEpochSeconds = windowStartEpochSeconds + windowSeconds;
        long expireSeconds = Math.max(1L, resetEpochSeconds - nowEpochSeconds);
        String clientKey = resolveClientKey(request);
        String redisKey = RedisKeys.gatewayRateLimit(matchedRule.getId(), clientKey, windowStartEpochSeconds);

        return incrementCounter(redisKey, expireSeconds)
                .flatMap(currentCount -> {
                    applyRateLimitHeaders(exchange.getResponse(), matchedRule, currentCount, resetEpochSeconds);
                    if (currentCount > matchedRule.getMaxRequests()) {
                        exchange.getResponse().getHeaders().set("Retry-After", String.valueOf(expireSeconds));
                        log.warn("gateway rate limited: rule={}, clientKey={}, method={}, path={}",
                                matchedRule.getId(), clientKey, method, request.getPath().value());
                        return GatewayResponseWriter.writeError(
                                exchange.getResponse(),
                                objectMapper,
                                HttpStatus.TOO_MANY_REQUESTS,
                                ResultCode.TOO_MANY_REQUESTS,
                                "Too many requests"
                        );
                    }
                    return chain.filter(exchange);
                })
                .onErrorResume(ex -> {
                    log.warn("gateway rate-limit fallback to pass-through: rule={}, clientKey={}, method={}, path={}",
                            matchedRule.getId(), clientKey, method, request.getPath().value(), ex);
                    return chain.filter(exchange);
                });
    }

    private Mono<Long> incrementCounter(String redisKey, long expireSeconds) {
        return reactiveStringRedisTemplate.opsForValue()
                .increment(redisKey)
                .flatMap(currentCount -> {
                    if (currentCount == null) {
                        return Mono.just(0L);
                    }
                    if (currentCount == 1L) {
                        return reactiveStringRedisTemplate.expire(redisKey, Duration.ofSeconds(expireSeconds))
                                .thenReturn(currentCount);
                    }
                    return Mono.just(currentCount);
                });
    }

    private void applyRateLimitHeaders(ServerHttpResponse response,
                                       RateLimitProperties.Rule rule,
                                       long currentCount,
                                       long resetEpochSeconds) {
        response.getHeaders().set("X-RateLimit-Limit", String.valueOf(rule.getMaxRequests()));
        response.getHeaders().set("X-RateLimit-Remaining",
                String.valueOf(Math.max(0L, rule.getMaxRequests() - currentCount)));
        response.getHeaders().set("X-RateLimit-Reset", String.valueOf(resetEpochSeconds));
    }

    private RateLimitProperties.Rule findMatchedRule(HttpMethod method, String path) {
        List<RateLimitProperties.Rule> rules = rateLimitProperties.getRules();
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        for (RateLimitProperties.Rule rule : rules) {
            if (rule == null || !StringUtils.hasText(rule.getPath())) {
                continue;
            }
            boolean pathMatched = antPathMatcher.match(rule.getPath().trim(), path);
            boolean methodMatched = !StringUtils.hasText(rule.getMethod())
                    || (method != null && method.name().equals(rule.getMethod().trim().toUpperCase(Locale.ROOT)));
            boolean validLimit = rule.getMaxRequests() > 0 && rule.getWindowSeconds() > 0;
            if (pathMatched && methodMatched && validLimit) {
                if (!StringUtils.hasText(rule.getId())) {
                    rule.setId((StringUtils.hasText(rule.getMethod()) ? rule.getMethod() : "ANY") + ":" + rule.getPath());
                }
                return rule;
            }
        }
        return null;
    }

    private String resolveClientKey(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
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
}
