package com.flashsale.productservice.domain.po;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * @author strive_qin
 * @version 1.0
 * @description ProductPO
 * @date 2026/3/20 00:00
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
