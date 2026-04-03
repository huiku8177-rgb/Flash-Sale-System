package com.flashsale.aiservice.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductKnowledgeDTO {

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

    public String toKnowledgeText() {
        StringBuilder sb = new StringBuilder();
        sb.append("商品名称：").append(name).append("\n");
        if (subtitle != null && !subtitle.isEmpty()) {
            sb.append("商品副标题：").append(subtitle).append("\n");
        }
        sb.append("价格：").append(price).append("元\n");
        if (marketPrice != null) {
            sb.append("市场价：").append(marketPrice).append("元\n");
        }
        if (stock != null) {
            sb.append("库存：").append(stock).append("件\n");
        }
        if (detail != null && !detail.isEmpty()) {
            sb.append("商品详情：").append(detail).append("\n");
        }
        return sb.toString();
    }
}
