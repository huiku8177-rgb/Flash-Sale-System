package com.flashsale.authservice.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
/**
 * @author strive_qin
 * @version 1.0
 * @description UserVO
 * @date 2026/3/20 00:00
 */


@Data
@Schema(description = "登录用户信息")
public class UserVO {

    @Schema(description = "用户ID", example = "10001")
    private Long userId;

    @Schema(description = "用户名", example = "neo")
    private String username;

    @Schema(description = "登录成功后返回的 JWT 令牌", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;
}
