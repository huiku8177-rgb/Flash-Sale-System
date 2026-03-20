package com.flashsale.productservice.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
/**
 * @author strive_qin
 * @version 1.0
 * @description NormalOrderItemRequestDTO
 * @date 2026/3/20 00:00
 */


@Data
@Schema(description = "普通订单商品项")
public class NormalOrderItemRequestDTO {

    @NotNull(message = "商品ID不能为空")
    @Positive(message = "商品ID必须大于0")
    @Schema(description = "普通商品ID", example = "1001")
    private Long productId;

    @NotNull(message = "购买数量不能为空")
    @Positive(message = "购买数量必须大于0")
    @Schema(description = "购买数量", example = "2")
    private Integer quantity;
}
