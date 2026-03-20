package com.flashsale.seckillservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        servers = {
                @Server(url = "/", description = "通过当前网关入口访问")
        },
        info = @Info(
                title = "闪购系统-秒杀服务接口文档",
                version = "v1",
                description = "秒杀商品与秒杀流程相关接口。受保护接口请通过网关并携带 Authorization Bearer Token 访问。"
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "请在网关请求头中传入 Authorization: Bearer <token>"
)
/**
 * @author strive_qin
 * @version 1.0
 * @description OpenApiConfig
 * @date 2026/3/20 00:00
 */

public class OpenApiConfig {
}
