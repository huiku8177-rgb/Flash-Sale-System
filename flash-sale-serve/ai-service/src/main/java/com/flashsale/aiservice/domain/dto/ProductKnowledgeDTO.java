package com.flashsale.aiservice.domain.dto;

import com.flashsale.aiservice.domain.enums.KnowledgeSourceType;
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

    public KnowledgeSourceType getKnowledgeSourceType() {
        return KnowledgeSourceType.PRODUCT;
    }

    public String toKnowledgeText() {
        StringBuilder builder = new StringBuilder();
        builder.append("\u5546\u54c1\u540d\u79f0: ").append(name).append("\n");
        if (subtitle != null && !subtitle.isEmpty()) {
            builder.append("\u5546\u54c1\u526f\u6807\u9898: ").append(subtitle).append("\n");
        }
        if (price != null) {
            builder.append("\u552e\u4ef7: ").append(price).append(" \u5143\n");
        }
        if (marketPrice != null) {
            builder.append("\u5212\u7ebf\u4ef7: ").append(marketPrice).append(" \u5143\n");
        }
        if (stock != null) {
            builder.append("\u5e93\u5b58: ").append(stock).append("\n");
        }
        if (status != null) {
            builder.append("\u72b6\u6001: ").append(status == 1 ? "\u4e0a\u67b6\u4e2d" : "\u5df2\u4e0b\u67b6").append("\n");
        }
        if (detail != null && !detail.isEmpty()) {
            builder.append("\u5546\u54c1\u8be6\u60c5: ").append(detail).append("\n");
        }
        return builder.toString();
    }
}
