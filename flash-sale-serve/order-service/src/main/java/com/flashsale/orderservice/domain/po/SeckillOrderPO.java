package com.flashsale.orderservice.domain.po;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀订单实体
 *
 * @author strive_qin
 * @version 1.0
 * @description SeckillOrderPO
 * @date 2026/3/13 17:00
 */
@Data
public class SeckillOrderPO {

    private Long id;

    private Long userId;

    private Long productId;

    private BigDecimal seckillPrice;

    private Integer status;

    private LocalDateTime createTime;
}
