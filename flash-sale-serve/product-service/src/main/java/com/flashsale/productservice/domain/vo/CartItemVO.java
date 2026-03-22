package com.flashsale.productservice.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author strive_qin
 * @version 1.0
 * @description CartItemVO
 * @date 2026/3/22 00:00
 */
@Data
@Schema(description = "购物车商品信息")
public class CartItemVO {

    @Schema(description = "购物车项ID", example = "1")
    private Long id;

    @Schema(description = "用户ID", example = "10001")
    private Long userId;

    @Schema(description = "普通商品ID", example = "1001")
    private Long productId;

    @Schema(description = "商品名称", example = "旗舰手机")
    private String productName;

    @Schema(description = "商品副标题", example = "12GB+256GB")
    private String productSubtitle;

    @Schema(description = "商品主图地址", example = "https://cdn.example.com/product/1001.png")
    private String productImage;

    @Schema(description = "商品售价", example = "99.50")
    private BigDecimal price;

    @Schema(description = "商品划线价", example = "129.00")
    private BigDecimal marketPrice;

    @Schema(description = "当前库存", example = "20")
    private Integer stock;

    @Schema(description = "商品状态：0-下架，1-上架", example = "1")
    private Integer status;

    @Schema(description = "购买数量", example = "2")
    private Integer quantity;

    @Schema(description = "是否选中", example = "true")
    private Boolean selected;

    @Schema(description = "当前行总金额", example = "199.00")
    private BigDecimal itemAmount;

    @Schema(description = "当前是否可参与结算", example = "true")
    private Boolean canCheckout;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
