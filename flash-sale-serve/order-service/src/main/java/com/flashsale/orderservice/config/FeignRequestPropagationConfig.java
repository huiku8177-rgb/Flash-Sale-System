package com.flashsale.orderservice.config;

import com.flashsale.common.web.RequestHeaderNames;
import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 透传基础链路头，方便跨服务排查订单链路问题。
 */
@Configuration
public class FeignRequestPropagationConfig {

    private static final String REQUEST_ID_HEADER = RequestHeaderNames.X_REQUEST_ID;
    private static final String USER_ID_HEADER = RequestHeaderNames.X_USER_ID;

    @Bean
    public RequestInterceptor requestPropagationInterceptor() {
        return requestTemplate -> {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
                return;
            }

            HttpServletRequest request = servletAttributes.getRequest();
            copyHeader(request, requestTemplate, REQUEST_ID_HEADER);
            copyHeader(request, requestTemplate, USER_ID_HEADER);
        };
    }

    private void copyHeader(HttpServletRequest request, feign.RequestTemplate requestTemplate, String headerName) {
        String headerValue = request.getHeader(headerName);
        if (headerValue != null && !headerValue.isBlank()) {
            requestTemplate.header(headerName, headerValue.trim());
        }
    }
}
