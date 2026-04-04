package com.flashsale.aiservice.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChatSessionVO {

    private String sessionId;
    private Long userId;
    private Long productId;
    private String contextType;
    private Integer messageCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;
    private List<ChatRecordVO> records;
}
