package com.flashsale.productservice.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
/**
 * @author strive_qin
 * @version 1.0
 * @description ProductQueryDTO
 * @date 2026/3/20 00:00
 */


@Data
@Schema(description = "普通商品列表查询参数")
public class ProductQueryDTO {

    @Schema(description = "商品名称关键字", example = "手机")
    private String name;

    @Schema(description = "商品状态，1-上架，0-下架", example = "1")
    private Integer status;

    @Schema(description = "分类ID", example = "10")
    private Long categoryId;
}
