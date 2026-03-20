package com.flashsale.productservice.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * @author strive_qin
 * @version 1.0
 * @description ProductVO
 * @date 2026/3/20 00:00
 */


@Data
@Schema(description = "普通商品信息")
public class ProductVO {

    @Schema(description = "商品ID", example = "1001")
    private Long id;

    @Schema(description = "商品名称", example = "旗舰手机")
    private String name;

    @Schema(description = "商品副标题", example = "12GB+256GB")
    private String subtitle;

    @Schema(description = "分类ID", example = "10")
    private Long categoryId;

    @Schema(description = "销售价", example = "4999.00")
    private BigDecimal price;

    @Schema(description = "市场价", example = "5499.00")
    private BigDecimal marketPrice;

    @Schema(description = "库存", example = "120")
    private Integer stock;

    @Schema(description = "商品状态，1-上架，0-下架", example = "1")
    private Integer status;

    @Schema(description = "商品主图地址", example = "https://cdn.example.com/product/1001.png")
    private String mainImage;

    @Schema(description = "商品详情")
    private String detail;

    @Schema(description = "创建时间", example = "2026-03-20T10:00:00")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2026-03-20T10:30:00")
    private LocalDateTime updateTime;
}
