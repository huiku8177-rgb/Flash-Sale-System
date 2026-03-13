package com.flashsale.seckillservice.domain.vo;

/**
 * @author strive_qin
 * @version 1.0
 * @description ProductVO
 * @date 2026/3/13 16:02
 */

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品返回 VO
 */
@Data
public class ProductVO {

    /**
     * 商品ID
     */
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 原价
     */
    private BigDecimal price;

    /**
     * 秒杀价
     */
    private BigDecimal seckillPrice;

    /**
     * 秒杀库存
     */
    private Integer stock;

    /**
     * 状态：0-下架，1-上架
     */
    private Integer status;

    /**
     * 秒杀开始时间
     */
    private LocalDateTime startTime;

    /**
     * 秒杀结束时间
     */
    private LocalDateTime endTime;
}
