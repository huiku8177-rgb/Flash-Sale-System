package com.flashsale.aiservice.domain.po;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ChatRecordPO {

    private Long id;
    private String sessionId;
    private Long userId;
    private Long productId;
    private Integer recordNo;
    private String question;
    private String questionCategory;
    private String answer;
    private String answerPolicy;
    private String sourcesJson;
    private String hitKnowledgeJson;
    private BigDecimal confidence;
    private String fallbackReason;
    private String auditSummary;
    private String modelName;
    private Integer latencyMs;
    private Integer estimatedTokens;
    private LocalDateTime createdAt;
    private LocalDateTime expireAt;
}
