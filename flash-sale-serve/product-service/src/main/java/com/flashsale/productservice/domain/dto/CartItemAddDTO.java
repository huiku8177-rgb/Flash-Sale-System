package com.flashsale.productservice.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author strive_qin
 * @version 1.0
 * @description CartItemAddDTO
 * @date 2026/3/22 00:00
 */
@Data
@Schema(description = "新增购物车商品请求")
public class CartItemAddDTO {

    @NotNull(message = "商品ID不能为空")
    @Min(value = 1, message = "商品ID必须大于等于1")
    @Schema(description = "普通商品ID", example = "1001")
    private Long productId;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于等于1")
    @Schema(description = "购买数量", example = "2")
    private Integer quantity;

    @Schema(description = "是否默认选中，默认选中", example = "true")
    private Boolean selected;
}
