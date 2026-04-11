package com.flashsale.aiservice.domain.dto;

import com.flashsale.aiservice.domain.enums.KnowledgeSourceType;
import lombok.Data;
import org.springframework.util.StringUtils;

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

    // Keep the legacy method for compatibility, but route it to stable retrieval text only.
    // This prevents dynamic facts from silently re-entering the embedding corpus.
    public String toKnowledgeText() {
        return toRetrievalText();
    }

    // Retrieval text is the stable product description used for embedding and RAG evidence.
    // Dynamic fields such as price, stock, and status are intentionally excluded here.
    public String toRetrievalText() {
        StringBuilder builder = new StringBuilder();
        appendLine(builder, "Product name", name);
        appendLine(builder, "Product subtitle", subtitle);
        appendLine(builder, "Product detail", detail);
        return builder.toString();
    }

    // Realtime facts are built separately so prompt assembly can keep [Evidence] and [Realtime facts] isolated.
    public String toRealtimeFacts() {
        StringBuilder builder = new StringBuilder();
        appendPrice(builder, "Realtime price", price);
        appendPrice(builder, "Reference market price", marketPrice);
        if (stock != null) {
            builder.append("Realtime stock: ").append(stock).append("\n");
        }
        if (status != null) {
            builder.append("Realtime status: ").append(status == 1 ? "available" : "unavailable").append("\n");
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
