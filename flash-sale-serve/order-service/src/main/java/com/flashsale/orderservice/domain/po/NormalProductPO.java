package com.flashsale.orderservice.domain.po;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 普通商品实体快照，仅供订单服务读取。
 */
@Data
public class NormalProductPO {

    private Long id;

    private String name;

    private String subtitle;

    private Long categoryId;

    private BigDecimal price;

    private BigDecimal marketPrice;

    private Integer stock;

    private Integer status;

    private String mainImage;

    private String detail;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
