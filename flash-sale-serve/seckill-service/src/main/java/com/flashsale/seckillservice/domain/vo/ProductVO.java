package com.flashsale.seckillservice.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 普通商品返回 VO
 */
@Data
public class ProductVO {

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
