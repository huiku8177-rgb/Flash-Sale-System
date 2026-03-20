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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
/**
 * @author strive_qin
 * @version 1.0
 * @description AuthGlobalFilter
 * @date 2026/3/20 00:00
 */


@Slf4j
@RequiredArgsConstructor
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final AuthProperties authProperties;
    private final JwtTool jwtTool;
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

        log.info("网关鉴权过滤器处理请求，path={}", path);

        // 预检请求直接放行，避免影响跨域协商
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // 白名单路径不做鉴权，例如登录、注册和文档接口
        if (isExclude(path)) {
            log.debug("当前路径命中白名单，跳过鉴权，path={}", path);
            return chain.filter(exchange);
        }

        // 从 Authorization 请求头中提取 Bearer Token
        String token = null;
        List<String> authorization = request.getHeaders().get("Authorization");
        if (authorization != null && !authorization.isEmpty()) {
            String authHeader = authorization.get(0);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        // 未携带令牌时直接返回 401
        if (token == null || token.isBlank()) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        // 解析令牌，拿到当前用户ID
        Long userId;
        try {
            userId = jwtTool.parseToken(token);
        } catch (UnauthorizedException e) {
            log.warn("令牌解析失败：{}", e.getMessage());
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        // 将用户ID透传给下游服务，避免各服务重复解析令牌
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(builder -> builder.header("X-User-Id", userId.toString()))
                .build();

        return chain.filter(mutatedExchange);
    }

    // 判断当前请求路径是否命中鉴权白名单
    private boolean isExclude(String path) {
        for (String pathPattern : authProperties.getExcludePaths() == null
                ? Collections.<String>emptyList()
                : authProperties.getExcludePaths()) {
            if (antPathMatcher.match(pathPattern, path)) {
                return true;
            }
        }
        return false;
    }

    // 鉴权过滤器优先执行
    @Override
    public int getOrder() {
        return 0;
    }
}
