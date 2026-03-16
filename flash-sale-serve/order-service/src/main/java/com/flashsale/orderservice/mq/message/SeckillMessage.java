package com.flashsale.orderservice.mq.message;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀异步消息体（order-service 侧）。
 *
 * <p>字段语义与 seckill-service 保持一致，便于 JSON 自动转换。</p>
 */
@Data
public class SeckillMessage implements Serializable {

    /** 消息唯一ID，用于消费幂等。 */
    private String messageId;

    /** 抢购用户ID。 */
    private Long userId;

    /** 秒杀商品ID。 */
    private Long productId;

    /** 抢购价格快照。 */
    private BigDecimal seckillPrice;

    /** 请求入队时间。 */
    private LocalDateTime createTime;

    private LocalDateTime expireAt;
}
