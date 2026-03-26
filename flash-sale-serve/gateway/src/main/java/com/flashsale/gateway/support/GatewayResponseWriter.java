package com.flashsale.gateway.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.domain.Result;
import com.flashsale.common.domain.ResultCode;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 统一输出网关错误响应。
 */
public final class GatewayResponseWriter {

    private GatewayResponseWriter() {
    }

    public static Mono<Void> writeError(ServerHttpResponse response,
                                        ObjectMapper objectMapper,
                                        HttpStatus status,
                                        ResultCode resultCode,
                                        String message) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Result<Void> result = Result.error(resultCode, message);
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(result);
        } catch (JsonProcessingException e) {
            body = ("{\"code\":" + resultCode.getCode() + ",\"message\":\"" + escapeJson(message) + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
