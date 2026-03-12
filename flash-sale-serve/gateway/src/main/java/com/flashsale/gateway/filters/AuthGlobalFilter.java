package com.flashsale.gateway.filters;

import cn.hutool.core.text.AntPathMatcher;
import com.flashsale.common.exception.UnauthorizedException;
import com.flashsale.common.util.JwtTool;
import com.flashsale.gateway.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author strive_qin
 * @version 1.0
 * @description AuthGlobalFilter
 * @date 2026/3/12 16:22
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    /** 鉴权白名单配置，例如 /auth/login、/auth/register。 */
    private final AuthProperties authProperties;
    /** JWT 解析工具，用于统一验证 token 并提取 userId。 */
    private final JwtTool jwtTool;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();

        log.info("网关鉴权过滤器拦截请求: {}", path);

        // 1. 白名单放行
        if (isExclude(path)) {
            log.debug("{} 不需要登录拦截", path);
            return chain.filter(exchange);
        }

        // 2. 获取 token
        String token = null;
        List<String> authorization = request.getHeaders().get("Authorization");
        if (authorization != null && !authorization.isEmpty()) {
            String authHeader = authorization.get(0);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        if (token == null || token.isBlank()) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        // 3. 校验 token
        Long userId;
        try {
            userId = jwtTool.parseToken(token);
        } catch (UnauthorizedException e) {
            log.warn("token 校验失败: {}", e.getMessage());
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        // 4. 传递用户信息给下游服务
        // 下游服务只需读取 X-User-Id，无需重复解析 JWT
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(builder -> builder.header("X-User-Id", userId.toString()))
                .build();

        return chain.filter(mutatedExchange);
    }

    private boolean isExclude(String path) {
        // 命中任意白名单规则即放行
        for (String pathPattern : authProperties.getExcludePaths()) {
            if (antPathMatcher.match(pathPattern, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
