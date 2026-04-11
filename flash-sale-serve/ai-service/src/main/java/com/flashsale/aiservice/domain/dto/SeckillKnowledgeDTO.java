package com.flashsale.aiservice.domain.dto;

import com.flashsale.aiservice.domain.enums.KnowledgeSourceType;
import lombok.Data;
import org.springframework.util.StringUtils;

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

    // Keep backward compatibility while ensuring embedding text never includes dynamic fields.
    public String toKnowledgeText() {
        return toRetrievalText();
    }

    // Seckill retrieval text must stay stable. Price, stock, and activity time belong to realtime facts.
    public String toRetrievalText() {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, "Seckill product", name);
        appendLine(builder, "Stable rule hint", "Use realtime facts for live price, stock, and activity window.");
        return builder.toString();
    }

    // Realtime facts are separated from retrieval evidence to avoid stale-vs-live conflicts in prompt assembly.
    public String toRealtimeFacts() {
        StringBuilder builder = new StringBuilder();
        appendPrice(builder, "Original price", price);
        appendPrice(builder, "Realtime seckill price", seckillPrice);
        if (stock != null) {
            builder.append("Realtime stock: ").append(stock).append("\n");
        }
        if (status != null) {
            builder.append("Realtime activity status: ").append(status == 1 ? "active" : "ended").append("\n");
        }
        if (startTime != null) {
            builder.append("Activity start time: ").append(startTime).append("\n");
        }
        if (endTime != null) {
            builder.append("Activity end time: ").append(endTime).append("\n");
        }
        return builder.toString();
    }

    private void appendLine(StringBuilder builder, String label, String value) {
        if (StringUtils.hasText(value)) {
            builder.append(label).append(": ").append(value.trim()).append("\n");
        }
    }

    private void appendPrice(StringBuilder builder, String label, BigDecimal value) {
        if (value != null) {
            builder.append(label).append(": ").append(value).append(" CNY\n");
        }
    }
}
