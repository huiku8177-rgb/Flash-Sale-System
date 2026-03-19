package com.flashsale.orderservice.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 秒杀订单支付状态返回对象。
 */
@Data
public class SeckillOrderPayStatusVO {

    private Long orderId;

    private Long productId;

    private Integer status;

    private Boolean paid;

    private BigDecimal seckillPrice;

    private String message;
}
