package com.flashsale.authservice.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
/**
 * @author strive_qin
 * @version 1.0
 * @description UpdatePasswordRequestDTO
 * @date 2026/3/20 00:00
 */


@Data
@Schema(description = "修改密码请求参数")
public class UpdatePasswordRequestDTO {

    @NotBlank
    @Size(min = 6, max = 64)
    @Schema(description = "旧密码", example = "OldPass@123")
    private String oldPassword;

    @NotBlank
    @Size(min = 6, max = 64)
    @Schema(description = "新密码", example = "NewPass@123")
    private String newPassword;
}
