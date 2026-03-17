package com.flashsale.seckillservice.domain.po;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀商品表实体
 */
@Data
public class SeckillProductPO {

    private Long id;

    private String name;

    private BigDecimal price;

    private BigDecimal seckillPrice;

    private Integer stock;

    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createTime;
}
