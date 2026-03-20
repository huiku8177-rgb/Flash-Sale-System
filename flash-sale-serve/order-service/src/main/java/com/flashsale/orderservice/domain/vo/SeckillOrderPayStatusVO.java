package com.flashsale.orderservice.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillOrderPayStatusVO
 * @date 2026/3/20 00:00
 */


@Data
@Schema(description = "秒杀订单支付状态")
public class SeckillOrderPayStatusVO {

    @Schema(description = "订单ID", example = "50001")
    private Long orderId;

    @Schema(description = "秒杀商品ID", example = "2001")
    private Long productId;

    @Schema(description = "订单状态", example = "1")
    private Integer status;

    @Schema(description = "是否已支付", example = "true")
    private Boolean paid;

    @Schema(description = "秒杀价格", example = "699.00")
    private BigDecimal seckillPrice;

    @Schema(description = "状态说明", example = "秒杀订单已支付")
    private String message;
}
