package com.flashsale.orderservice.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 普通订单支付状态返回对象。
 */
@Data
public class NormalOrderPayStatusVO {

    private Long orderId;

    private String orderNo;

    private Integer orderStatus;

    private Boolean paid;

    private BigDecimal payAmount;

    private LocalDateTime payTime;

    private String message;
}
