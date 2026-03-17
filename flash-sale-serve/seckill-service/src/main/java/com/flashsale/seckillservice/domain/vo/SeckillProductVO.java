package com.flashsale.seckillservice.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀商品返回 VO
 */
@Data
public class SeckillProductVO {

    private Long id;

    private String name;

    private BigDecimal price;

    private BigDecimal seckillPrice;

    private Integer stock;

    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
