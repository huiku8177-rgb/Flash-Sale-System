package com.flashsale.orderservice.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * @author strive_qin
 * @version 1.0
 * @description NormalOrderItemVO
 * @date 2026/3/20 00:00
 */


@Data
@Schema(description = "普通订单商品项详情")
public class NormalOrderItemVO {

    @Schema(description = "明细ID", example = "31001")
    private Long id;

    @Schema(description = "订单ID", example = "30001")
    private Long orderId;

    @Schema(description = "用户ID", example = "10001")
    private Long userId;

    @Schema(description = "商品ID", example = "1001")
    private Long productId;

    @Schema(description = "商品名称", example = "旗舰手机")
    private String productName;

    @Schema(description = "商品副标题", example = "12GB+256GB")
    private String productSubtitle;

    @Schema(description = "商品图片地址", example = "https://cdn.example.com/product/1001.png")
    private String productImage;

    @Schema(description = "销售单价", example = "99.50")
    private BigDecimal salePrice;

    @Schema(description = "购买数量", example = "2")
    private Integer quantity;

    @Schema(description = "明细金额", example = "199.00")
    private BigDecimal itemAmount;

    @Schema(description = "创建时间", example = "2026-03-20T17:30:00")
    private LocalDateTime createTime;
}
