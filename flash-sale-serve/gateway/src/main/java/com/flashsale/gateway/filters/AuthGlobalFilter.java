package com.flashsale.gateway.filters;

import cn.hutool.core.text.AntPathMatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.domain.ResultCode;
import com.flashsale.common.exception.UnauthorizedException;
import com.flashsale.common.util.JwtTool;
import com.flashsale.common.web.RequestHeaderNames;
import com.flashsale.gateway.config.AuthProperties;
import com.flashsale.gateway.support.GatewayResponseWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 网关统一鉴权过滤器。
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String AUTHORIZATION_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = RequestHeaderNames.X_USER_ID;

    private final AuthProperties authProperties;
    private final JwtTool jwtTool;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();
        HttpMethod method = request.getMethod();
        String requestId = exchange.getAttributeOrDefault(
                RequestTraceGlobalFilter.REQUEST_ID_ATTR,
                request.getHeaders().getFirst(RequestTraceGlobalFilter.REQUEST_ID_HEADER)
        );

        if (method == HttpMethod.OPTIONS || isExcluded(method, path)) {
            return chain.filter(exchange);
        }

        String token = resolveBearerToken(request.getHeaders());
        if (token == null || token.isBlank()) {
            return GatewayResponseWriter.writeError(
                    exchange.getResponse(),
                    objectMapper,
                    HttpStatus.UNAUTHORIZED,
                    ResultCode.UNAUTHORIZED,
                    "未登录或登录已失效"
            );
        }

        Long userId;
        try {
            userId = jwtTool.parseToken(token);
        } catch (UnauthorizedException e) {
            log.warn("gateway auth failed: requestId={}, method={}, path={}, message={}",
                    requestId, method, path, e.getMessage());
            return GatewayResponseWriter.writeError(
                    exchange.getResponse(),
                    objectMapper,
                    HttpStatus.UNAUTHORIZED,
                    ResultCode.UNAUTHORIZED,
                    "登录令牌无效或已过期"
            );
        }

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(builder -> builder.headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.set(USER_ID_HEADER, userId.toString());
                }))
                .build();

        return chain.filter(mutatedExchange);
    }

    private String resolveBearerToken(HttpHeaders headers) {
        List<String> authorizationHeaders = headers.get(HttpHeaders.AUTHORIZATION);
        if (authorizationHeaders == null || authorizationHeaders.isEmpty()) {
            return null;
        }

        String authHeader = authorizationHeaders.get(0);
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }

        if (authHeader.regionMatches(true, 0, AUTHORIZATION_PREFIX, 0, AUTHORIZATION_PREFIX.length())) {
            return authHeader.substring(AUTHORIZATION_PREFIX.length()).trim();
        }
        return null;
    }

    private boolean isExcluded(HttpMethod method, String path) {
        for (String rule : authProperties.getExcludePaths() == null
                ? Collections.<String>emptyList()
                : authProperties.getExcludePaths()) {
            if (matchesRule(method, path, rule)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesRule(HttpMethod method, String path, String rule) {
        if (rule == null || rule.isBlank()) {
            return false;
        }

        int separatorIndex = rule.indexOf(':');
        if (separatorIndex <= 0) {
            return antPathMatcher.match(rule, path);
        }

        String methodRule = rule.substring(0, separatorIndex).trim().toUpperCase(Locale.ROOT);
        String pathRule = rule.substring(separatorIndex + 1).trim();
        if (pathRule.isBlank()) {
            return false;
        }

        return method != null
                && method.name().equals(methodRule)
                && antPathMatcher.match(pathRule, path);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
