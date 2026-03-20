package com.flashsale.authservice.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
/**
 * @author strive_qin
 * @version 1.0
 * @description RegisterRequestDTO
 * @date 2026/3/20 00:00
 */


@Data
@Schema(description = "注册请求参数")
public class RegisterRequestDTO {

    @NotBlank
    @Size(min = 3, max = 32)
    @Schema(description = "用户名", example = "neo")
    private String username;

    @NotBlank
    @Size(min = 6, max = 64)
    @Schema(description = "密码", example = "FlashSale@123")
    private String password;
}
