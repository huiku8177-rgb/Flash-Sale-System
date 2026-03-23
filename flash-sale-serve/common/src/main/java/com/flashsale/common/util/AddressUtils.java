package com.flashsale.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.util.StringUtils;

/**
 * 地址工具类。
 *
 * @author strive_qin
 * @version 1.0
 * @description AddressUtils
 * @date 2026/3/23 00:00
 */
public final class AddressUtils {

    private AddressUtils() {
    }

    public static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public static boolean hasRequiredFields(String receiver, String mobile, String detail) {
        return StringUtils.hasText(receiver)
                && StringUtils.hasText(mobile)
                && StringUtils.hasText(detail);
    }

    public static boolean isMobileValid(String mobile) {
        return StringUtils.hasText(mobile) && mobile.matches("^1\\d{10}$");
    }

    public static String buildSnapshot(String receiver,
                                       String mobile,
                                       String detail,
                                       ObjectMapper objectMapper) throws JsonProcessingException {
        ObjectNode addressNode = objectMapper.createObjectNode();
        addressNode.put("receiver", trimToNull(receiver));
        addressNode.put("mobile", trimToNull(mobile));
        addressNode.put("detail", trimToNull(detail));
        return objectMapper.writeValueAsString(addressNode);
    }

    public static JsonNode parseSnapshot(String addressSnapshot, ObjectMapper objectMapper) throws JsonProcessingException {
        return objectMapper.readTree(addressSnapshot);
    }

    public static String normalizeSnapshot(String addressSnapshot, ObjectMapper objectMapper) throws JsonProcessingException {
        return objectMapper.writeValueAsString(parseSnapshot(addressSnapshot, objectMapper));
    }

    public static boolean hasRequiredFields(JsonNode addressNode) {
        return hasText(addressNode, "receiver")
                && hasText(addressNode, "mobile")
                && hasText(addressNode, "detail");
    }

    public static String summary(String receiver, String mobile, String detail) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(receiver)) {
            builder.append(receiver.trim());
        }
        if (StringUtils.hasText(mobile)) {
            if (!builder.isEmpty()) {
                builder.append(" ");
            }
            builder.append(mobile.trim());
        }
        if (StringUtils.hasText(detail)) {
            if (!builder.isEmpty()) {
                builder.append(" ");
            }
            builder.append(detail.trim());
        }
        return builder.toString();
    }

    private static boolean hasText(JsonNode addressNode, String fieldName) {
        return addressNode != null && StringUtils.hasText(addressNode.path(fieldName).asText());
    }
}
