package com.flashsale.authservice.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author strive_qin
 * @version 1.0
 * @description UserAddressVO
 * @date 2026/3/23 00:00
 */
@Data
@Schema(description = "用户收货地址")
public class UserAddressVO {

    @Schema(description = "地址ID", example = "1")
    private Long id;

    @Schema(description = "用户ID", example = "10001")
    private Long userId;

    @Schema(description = "收货人", example = "张三")
    private String receiver;

    @Schema(description = "手机号", example = "13800000000")
    private String mobile;

    @Schema(description = "详细地址", example = "深圳市南山区科技园某路100号")
    private String detail;

    @Schema(description = "是否默认地址", example = "true")
    private Boolean isDefault;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
