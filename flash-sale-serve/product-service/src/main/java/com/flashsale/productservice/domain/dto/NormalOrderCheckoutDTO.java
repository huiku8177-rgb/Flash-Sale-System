package com.flashsale.productservice.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author strive_qin
 * @version 1.0
 * @description NormalOrderCheckoutDTO
 * @date 2026/3/20 00:00
 */
@Data
@Schema(description = "普通订单创建请求")
public class NormalOrderCheckoutDTO {

    @NotNull(message = "收货地址ID不能为空")
    @Min(value = 1, message = "收货地址ID必须大于等于1")
    @Schema(description = "收货地址ID", example = "1")
    private Long addressId;

    @Size(max = 200, message = "订单备注长度不能超过200个字符")
    @Schema(description = "订单备注", example = "请尽快发货")
    private String remark;
}
