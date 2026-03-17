package com.flashsale.seckillservice.domain.po;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 普通商品表实体
 */
@Data
public class ProductPO {

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
