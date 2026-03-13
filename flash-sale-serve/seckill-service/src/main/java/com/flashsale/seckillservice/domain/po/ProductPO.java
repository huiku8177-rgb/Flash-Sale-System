package com.flashsale.seckillservice.domain.po;

/**
 * @author strive_qin
 * @version 1.0
 * @description ProductPO
 * @date 2026/3/13 16:00
 */
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品表实体
 */
@Data
public class ProductPO {

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

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
