package com.flashsale.orderservice.mq.message;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillMessage
 * @date 2026/3/20 00:00
 */


@Data
public class SeckillMessage implements Serializable {

    private String messageId;
    private Long userId;
    private Long productId;
    private BigDecimal seckillPrice;
    private LocalDateTime createTime;
    private LocalDateTime expireAt;
}
