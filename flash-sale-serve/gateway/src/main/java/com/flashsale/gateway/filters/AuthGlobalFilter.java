package com.flashsale.gateway.filters;

import cn.hutool.core.text.AntPathMatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.domain.ResultCode;
import com.flashsale.common.exception.UnauthorizedException;
import com.flashsale.common.redis.RedisKeys;
import com.flashsale.common.util.JwtTool;
import com.flashsale.common.web.RequestHeaderNames;
import com.flashsale.gateway.config.AuthProperties;
import com.flashsale.gateway.support.GatewayResponseWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Slf4j
@RequiredArgsConstructor
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String AUTHORIZATION_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = RequestHeaderNames.X_USER_ID;

    private final AuthProperties authProperties;
    private final JwtTool jwtTool;
    private final ObjectMapper objectMapper;
    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;
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
        if (!StringUtils.hasText(token)) {
            return unauthorized(exchange, "Login required");
        }

        JwtTool.TokenClaims claims;
        try {
            claims = jwtTool.parseTokenClaims(token);
        } catch (UnauthorizedException ex) {
            log.warn("gateway auth failed: requestId={}, method={}, path={}, message={}",
                    requestId, method, path, ex.getMessage());
            return unauthorized(exchange, "Invalid or expired token");
        }

        return validateSession(token, claims)
                .flatMap(validation -> {
                    if (!validation.valid()) {
                        log.warn("gateway session rejected: requestId={}, method={}, path={}, reason={}",
                                requestId, method, path, validation.reason());
                        return unauthorized(exchange, validation.reason());
                    }

                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(builder -> builder.headers(headers -> {
                                headers.remove(USER_ID_HEADER);
                                headers.set(USER_ID_HEADER, claims.userId().toString());
                            }))
                            .build();
                    return chain.filter(mutatedExchange);
                })
                .onErrorResume(ex -> {
                    log.error("gateway session validation failed: requestId={}, method={}, path={}",
                            requestId, method, path, ex);
                    return unauthorized(exchange, "Session validation failed");
                });
    }

    private Mono<SessionValidationResult> validateSession(String token, JwtTool.TokenClaims claims) {
        Mono<Boolean> blacklistedMono = reactiveStringRedisTemplate.opsForValue()
                .get(RedisKeys.authTokenBlacklist(token))
                .map(value -> true)
                .defaultIfEmpty(false);

        Mono<Long> versionMono = reactiveStringRedisTemplate.opsForValue()
                .get(RedisKeys.authTokenVersion(claims.userId()))
                .defaultIfEmpty("0")
                .map(this::parseTokenVersion);

        return Mono.zip(blacklistedMono, versionMono)
                .map(tuple -> {
                    if (Boolean.TRUE.equals(tuple.getT1())) {
                        return SessionValidationResult.rejected("Token has been revoked");
                    }
                    if (tuple.getT2() != claims.tokenVersion()) {
                        return SessionValidationResult.rejected("Session has changed, please log in again");
                    }
                    return SessionValidationResult.accepted();
                });
    }

    private long parseTokenVersion(String value) {
        if (!StringUtils.hasText(value)) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new UnauthorizedException("Invalid session state", ex);
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        return GatewayResponseWriter.writeError(
                exchange.getResponse(),
                objectMapper,
                HttpStatus.UNAUTHORIZED,
                ResultCode.UNAUTHORIZED,
                message
        );
    }

    private String resolveBearerToken(HttpHeaders headers) {
        List<String> authorizationHeaders = headers.get(HttpHeaders.AUTHORIZATION);
        if (authorizationHeaders == null || authorizationHeaders.isEmpty()) {
            return null;
        }

        String authHeader = authorizationHeaders.get(0);
        if (!StringUtils.hasText(authHeader)) {
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
        if (!StringUtils.hasText(rule)) {
            return false;
        }

        int separatorIndex = rule.indexOf(':');
        if (separatorIndex <= 0) {
            return antPathMatcher.match(rule, path);
        }

        String methodRule = rule.substring(0, separatorIndex).trim().toUpperCase(Locale.ROOT);
        String pathRule = rule.substring(separatorIndex + 1).trim();
        if (!StringUtils.hasText(pathRule)) {
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

    private record SessionValidationResult(boolean valid, String reason) {

        private static SessionValidationResult accepted() {
            return new SessionValidationResult(true, "OK");
        }

        private static SessionValidationResult rejected(String reason) {
            return new SessionValidationResult(false, reason);
        }
    }
}
