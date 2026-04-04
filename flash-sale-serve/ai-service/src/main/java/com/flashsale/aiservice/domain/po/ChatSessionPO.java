package com.flashsale.aiservice.domain.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatSessionPO {

    private Long id;
    private String sessionId;
    private Long userId;
    private Long productId;
    private String contextType;
    private Integer sessionStatus;
    private Integer messageCount;
    private String lastQuestion;
    private String lastAnswerSummary;
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;
    private LocalDateTime expireAt;
}
