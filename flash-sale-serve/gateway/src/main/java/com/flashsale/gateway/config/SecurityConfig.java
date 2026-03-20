
package com.flashsale.gateway.config;


import com.flashsale.common.util.JwtTool;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.rsa.crypto.KeyStoreKeyFactory;

import java.security.KeyPair;

/**
 * 网关安全配置
 *
 * @author strive_qin
 * @version 1.0
 * @description SecurityConfig
 * @date 2026/3/20 00:00
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    // 提供统一的密码加密器
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    // 基于项目密钥对创建 JWT 工具类
    @Bean
    public JwtTool jwtTool(KeyPair keyPair) {
        return new JwtTool(keyPair);
    }

    // 从 keystore 中加载 RSA 密钥对，用于 JWT 签发与验签
    @Bean
    public KeyPair keyPair(JwtProperties properties){
        // 获取秘钥工厂
        KeyStoreKeyFactory keyStoreKeyFactory =
                new KeyStoreKeyFactory(
                        properties.getLocation(),
                        properties.getPassword().toCharArray());
        //读取钥匙对
        return keyStoreKeyFactory.getKeyPair(
                properties.getAlias(),
                properties.getPassword().toCharArray());
    }
}
