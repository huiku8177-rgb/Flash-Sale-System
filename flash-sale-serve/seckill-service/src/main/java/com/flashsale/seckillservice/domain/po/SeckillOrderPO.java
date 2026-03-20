package com.flashsale.seckillservice.domain.po;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀订单表实体
 *
 * @author strive_qin
 * @version 1.0
 * @description SeckillOrderPO
 * @date 2026/3/13 16:00
 */
@Data
public class SeckillOrderPO {

    /**
     * 订单ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 下单时秒杀价
     */
    private BigDecimal seckillPrice;

    /**
     * 订单状态：0-新建，1-已支付，2-已取消
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
