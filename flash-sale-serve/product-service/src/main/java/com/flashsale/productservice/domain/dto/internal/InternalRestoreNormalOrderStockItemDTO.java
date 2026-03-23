package com.flashsale.productservice.domain.dto.internal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author strive_qin
 * @version 1.0
 * @description InternalRestoreNormalOrderStockItemDTO
 * @date 2026/3/23 00:00
 */
@Data
public class InternalRestoreNormalOrderStockItemDTO {

    @NotNull(message = "商品ID不能为空")
    @Min(value = 1, message = "商品ID必须大于等于1")
    private Long productId;

    @NotNull(message = "恢复数量不能为空")
    @Min(value = 1, message = "恢复数量必须大于等于1")
    private Integer quantity;
}
