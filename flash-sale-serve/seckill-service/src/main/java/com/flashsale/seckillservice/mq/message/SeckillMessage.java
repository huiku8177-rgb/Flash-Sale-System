package com.flashsale.seckillservice.mq.message;

/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillMessage
 * @date 2026/3/16 12:31
 */

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀消息
 */
@Data
public class SeckillMessage implements Serializable {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;

    /**
     * 请求时间
     */
    private LocalDateTime createTime;
}
