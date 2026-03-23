package com.flashsale.authservice.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author strive_qin
 * @version 1.0
 * @description UserAddressCreateDTO
 * @date 2026/3/23 00:00
 */
@Data
@Schema(description = "新增收货地址请求")
public class UserAddressCreateDTO {

    @NotBlank(message = "收货人不能为空")
    @Size(max = 64, message = "收货人长度不能超过64个字符")
    @Schema(description = "收货人", example = "张三")
    private String receiver;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13800000000")
    private String mobile;

    @NotBlank(message = "收货地址不能为空")
    @Size(max = 255, message = "收货地址长度不能超过255个字符")
    @Schema(description = "详细地址", example = "深圳市南山区科技园某路100号")
    private String detail;

    @Schema(description = "是否设为默认地址", example = "true")
    private Boolean isDefault;
}
