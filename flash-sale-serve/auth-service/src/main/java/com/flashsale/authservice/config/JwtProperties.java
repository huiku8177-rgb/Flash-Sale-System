package com.flashsale.authservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 认证服务 JWT 配置属性
 *
 * @author strive_qin
 * @version 1.0
 * @description JwtProperties
 * @date 2026/3/20 00:00
 */
@Data
@ConfigurationProperties(prefix = "flash-sale.jwt")
public class JwtProperties {
    private Resource location;
    private String password;
    private String alias;
    private Duration tokenTTL = Duration.ofMinutes(10);
}
