package com.flashsale.productservice.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
/**
 * @author strive_qin
 * @version 1.0
 * @description CreateNormalOrderItemDTO
 * @date 2026/3/20 00:00
 */


@Data
public class CreateNormalOrderItemDTO {

    private Long productId;

    private String productName;

    private String productSubtitle;

    private String productImage;

    private BigDecimal salePrice;

    private Integer quantity;

    private BigDecimal itemAmount;
}
