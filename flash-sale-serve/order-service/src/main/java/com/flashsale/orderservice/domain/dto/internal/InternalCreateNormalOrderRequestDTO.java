package com.flashsale.orderservice.domain.dto.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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

    @Size(max = 2000, message = "地址快照长度不能超过2000个字符")
    private String addressSnapshot;

    @NotEmpty(message = "订单商品不能为空")
    @Valid
    private List<InternalCreateNormalOrderItemDTO> items;
}
