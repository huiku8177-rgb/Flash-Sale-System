package com.flashsale.gateway.filters;

import com.flashsale.common.web.RequestHeaderNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 为所有网关请求补充并透传请求 ID。
 */
@Slf4j
@Component
public class RequestTraceGlobalFilter implements GlobalFilter, Ordered {

    public static final String REQUEST_ID_HEADER = RequestHeaderNames.X_REQUEST_ID;
    public static final String REQUEST_ID_ATTR = "flash-sale.request-id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestId = normalizeRequestId(request.getHeaders().getFirst(REQUEST_ID_HEADER));

        exchange.getAttributes().put(REQUEST_ID_ATTR, requestId);

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(builder -> builder.headers(headers -> headers.set(REQUEST_ID_HEADER, requestId)))
                .build();

        mutatedExchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, requestId);
        log.debug("gateway request start: requestId={}, method={}, path={}",
                requestId,
                request.getMethod(),
                request.getPath());
        return chain.filter(mutatedExchange);
    }

    private String normalizeRequestId(String requestId) {
        if (requestId == null) {
            return UUID.randomUUID().toString();
        }
        String trimmed = requestId.trim();
        if (trimmed.isEmpty() || trimmed.length() > 64) {
            return UUID.randomUUID().toString();
        }
        return trimmed;
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
