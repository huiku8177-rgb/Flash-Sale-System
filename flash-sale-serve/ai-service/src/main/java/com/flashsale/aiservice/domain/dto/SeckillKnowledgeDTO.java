package com.flashsale.aiservice.domain.dto;

import com.flashsale.aiservice.domain.enums.KnowledgeSourceType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillKnowledgeDTO {

    private Long id;
    private String name;
    private BigDecimal price;
    private BigDecimal seckillPrice;
    private Integer stock;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public KnowledgeSourceType getKnowledgeSourceType() {
        return KnowledgeSourceType.SECKILL;
    }

    public String toKnowledgeText() {
        StringBuilder builder = new StringBuilder();
        builder.append("\u79d2\u6740\u5546\u54c1: ").append(name).append("\n");
        if (price != null) {
            builder.append("\u539f\u4ef7: ").append(price).append(" \u5143\n");
        }
        if (seckillPrice != null) {
            builder.append("\u79d2\u6740\u4ef7: ").append(seckillPrice).append(" \u5143\n");
        }
        if (stock != null) {
            builder.append("\u5e93\u5b58: ").append(stock).append("\n");
        }
        if (status != null) {
            builder.append("\u72b6\u6001: ").append(status == 1 ? "\u6d3b\u52a8\u4e2d" : "\u5df2\u7ed3\u675f").append("\n");
        }
        if (startTime != null) {
            builder.append("\u5f00\u59cb\u65f6\u95f4: ").append(startTime).append("\n");
        }
        if (endTime != null) {
            builder.append("\u7ed3\u675f\u65f6\u95f4: ").append(endTime).append("\n");
        }
        return builder.toString();
    }
}
