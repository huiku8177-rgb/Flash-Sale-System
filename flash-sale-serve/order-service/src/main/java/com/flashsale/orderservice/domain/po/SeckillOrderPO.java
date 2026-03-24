package com.flashsale.orderservice.domain.po;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillOrderPO
 * @date 2026/3/13 17:00
 */
@Data
public class SeckillOrderPO {

    private Long id;

    private String orderNo;

    private Long userId;

    private Long productId;

    private BigDecimal seckillPrice;

    /**
     * 0-待支付，1-已支付，2-已取消
     */
    private Integer status;

    private LocalDateTime payTime;

    private String cancelReason;

    private LocalDateTime cancelTime;

    private LocalDateTime createTime;
}
