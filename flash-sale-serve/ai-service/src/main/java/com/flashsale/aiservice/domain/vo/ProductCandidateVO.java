package com.flashsale.aiservice.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "商品候选项")
public class ProductCandidateVO {

    @Schema(description = "商品 ID", example = "1001")
    private Long productId;

    @Schema(description = "商品类型", example = "normal")
    private String productType;

    @Schema(description = "商品名称", example = "iPhone 15")
    private String name;

    @Schema(description = "商品副标题")
    private String subtitle;

    @Schema(description = "展示价格")
    private BigDecimal price;

    @Schema(description = "匹配得分", example = "0.92")
    private double score;
}
