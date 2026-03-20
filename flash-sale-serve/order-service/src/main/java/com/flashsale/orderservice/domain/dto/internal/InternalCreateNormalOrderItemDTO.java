package com.flashsale.orderservice.domain.dto.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
/**
 * @author strive_qin
 * @version 1.0
 * @description InternalCreateNormalOrderItemDTO
 * @date 2026/3/20 00:00
 */


@Data
public class InternalCreateNormalOrderItemDTO {

    @NotNull(message = "商品ID不能为空")
    @Positive(message = "商品ID必须大于0")
    private Long productId;

    @NotBlank(message = "商品名称不能为空")
    private String productName;

    private String productSubtitle;

    private String productImage;

    @NotNull(message = "销售单价不能为空")
    @Positive(message = "销售单价必须大于0")
    private BigDecimal salePrice;

    @NotNull(message = "购买数量不能为空")
    @Positive(message = "购买数量必须大于0")
    private Integer quantity;

    @NotNull(message = "明细金额不能为空")
    @Positive(message = "明细金额必须大于0")
    private BigDecimal itemAmount;
}
