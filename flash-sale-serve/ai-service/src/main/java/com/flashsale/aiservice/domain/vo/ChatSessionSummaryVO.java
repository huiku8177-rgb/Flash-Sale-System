package com.flashsale.aiservice.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatSessionSummaryVO {

    private String sessionId;
    private Long userId;
    private Long productId;
    private String contextType;
    private Integer messageCount;
    private String lastQuestion;
    private String lastAnswerSummary;
    private String currentProductName;
    private String currentIntentType;
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;
}
