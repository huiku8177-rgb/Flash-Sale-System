package com.flashsale.orderservice.config;

import com.flashsale.common.web.RequestHeaderNames;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 为 order-service 统一补充请求 ID，便于串联网关、商品和订单日志。
 */
@Component
public class RequestTraceFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = RequestHeaderNames.X_REQUEST_ID;
    private static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = normalizeRequestId(request.getHeader(REQUEST_ID_HEADER));
        response.setHeader(REQUEST_ID_HEADER, requestId);
        MDC.put(MDC_KEY, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
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
}
