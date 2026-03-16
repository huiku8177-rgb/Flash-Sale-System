package com.flashsale.orderservice.mq.message;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author strive_qin
 * @version 1.0
 * @description SeckillMessage
 * @date 2026/3/16 12:51
 */
@Data
public class SeckillMessage implements Serializable {

    private Long userId;

    private Long productId;

    private BigDecimal seckillPrice;

    private LocalDateTime createTime;
}
