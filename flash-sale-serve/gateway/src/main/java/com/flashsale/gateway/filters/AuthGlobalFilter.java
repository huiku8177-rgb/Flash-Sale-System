package com.flashsale.gateway.filters;

import cn.hutool.core.text.AntPathMatcher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.domain.Result;
import com.flashsale.common.domain.ResultCode;
import com.flashsale.common.exception.UnauthorizedException;
import com.flashsale.common.util.JwtTool;
import com.flashsale.gateway.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 网关统一鉴权过滤器
 *
 * @author strive_qin
 * @version 1.0
 * @description AuthGlobalFilter
 * @date 2026/3/20 00:00
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String AUTHORIZATION_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = "X-User-Id";

    private final AuthProperties authProperties;
    private final JwtTool jwtTool;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    /**
     * 网关统一鉴权入口
     *
     * @param exchange 当前请求上下文
     * @param chain 过滤器链
     * @return 过滤结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();
        HttpMethod method = request.getMethod();

        log.debug("网关鉴权过滤器处理请求，method={}, path={}", method, path);

        if (method == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        if (isExcluded(method, path)) {
            return chain.filter(exchange);
        }

        String token = resolveBearerToken(request.getHeaders());
        if (token == null || token.isBlank()) {
            return writeUnauthorizedResponse(exchange.getResponse(), "未登录或登录已失效");
        }

        Long userId;
        try {
            userId = jwtTool.parseToken(token);
        } catch (UnauthorizedException e) {
            log.warn("令牌解析失败，method={}, path={}, message={}", method, path, e.getMessage());
            return writeUnauthorizedResponse(exchange.getResponse(), "登录令牌无效或已过期");
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
        if (authHeader == null || !authHeader.startsWith(AUTHORIZATION_PREFIX)) {
            return null;
        }
        return authHeader.substring(AUTHORIZATION_PREFIX.length());
    }

    private Mono<Void> writeUnauthorizedResponse(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Result<Void> result = Result.error(ResultCode.UNAUTHORIZED, message);
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(result);
        } catch (JsonProcessingException e) {
            body = ("{\"code\":401,\"message\":\"" + message + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
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

    /**
     * 支持两种白名单规则：
     * 1. /swagger-ui/**            -> 任意请求方法匹配
     * 2. GET:/product/products/** -> 仅指定请求方法匹配
     */
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
