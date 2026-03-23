package com.flashsale.orderservice.domain.dto.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author strive_qin
 * @version 1.0
 * @description InternalCreateNormalOrderRequestDTO
 * @date 2026/3/20 00:00
 */
@Data
public class InternalCreateNormalOrderRequestDTO {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotNull(message = "用户ID不能为空")
    @Positive(message = "用户ID必须大于0")
    private Long userId;

    @NotNull(message = "订单总金额不能为空")
    @Positive(message = "订单总金额必须大于0")
    private BigDecimal totalAmount;

    @NotNull(message = "实付金额不能为空")
    @Positive(message = "实付金额必须大于0")
    private BigDecimal payAmount;

    @Size(max = 200, message = "订单备注长度不能超过200个字符")
    private String remark;

    @NotBlank(message = "收货人不能为空")
    @Size(max = 64, message = "收货人长度不能超过64个字符")
    private String receiver;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String mobile;

    @NotBlank(message = "收货地址不能为空")
    @Size(max = 255, message = "收货地址长度不能超过255个字符")
    private String detail;

    @NotEmpty(message = "订单商品不能为空")
    @Valid
    private List<InternalCreateNormalOrderItemDTO> items;
}
