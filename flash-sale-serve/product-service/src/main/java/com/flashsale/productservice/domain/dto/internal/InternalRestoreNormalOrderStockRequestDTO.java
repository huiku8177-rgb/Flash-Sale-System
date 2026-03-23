package com.flashsale.productservice.domain.dto.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @author strive_qin
 * @version 1.0
 * @description InternalRestoreNormalOrderStockRequestDTO
 * @date 2026/3/23 00:00
 */
@Data
public class InternalRestoreNormalOrderStockRequestDTO {

    @NotNull(message = "用户ID不能为空")
    @Min(value = 1, message = "用户ID必须大于等于1")
    private Long userId;

    private String orderNo;

    @Valid
    @NotEmpty(message = "恢复库存商品不能为空")
    private List<InternalRestoreNormalOrderStockItemDTO> items;
}
