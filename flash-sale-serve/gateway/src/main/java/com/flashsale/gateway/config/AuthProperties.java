package com.flashsale.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 网关鉴权路径配置
 *
 * @author strive_qin
 * @version 1.0
 * @description AuthProperties
 * @date 2026/3/20 00:00
 */
@Data
@ConfigurationProperties(prefix = "flash-sale.auth")
@Component
public class AuthProperties {
    private List<String> excludePaths;
}
