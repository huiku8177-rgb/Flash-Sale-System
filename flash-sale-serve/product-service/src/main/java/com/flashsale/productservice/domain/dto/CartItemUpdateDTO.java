package com.flashsale.productservice.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * @author strive_qin
 * @version 1.0
 * @description CartItemUpdateDTO
 * @date 2026/3/22 00:00
 */
@Data
@Schema(description = "更新购物车商品请求")
public class CartItemUpdateDTO {

    @Min(value = 1, message = "购买数量必须大于等于1")
    @Schema(description = "新的购买数量，不传则保持原值", example = "3")
    private Integer quantity;

    @Schema(description = "是否选中，不传则保持原值", example = "false")
    private Boolean selected;
}
