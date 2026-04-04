package com.flashsale.aiservice.service;

import com.flashsale.aiservice.domain.po.ChatSessionPO;

import java.time.LocalDateTime;

public interface ChatSessionService {

    ChatSessionPO getOrCreate(String sessionId, Long userId, Long productId, String contextType);

    ChatSessionPO getRequired(String sessionId, Long userId);

    void refreshSession(ChatSessionPO session, String lastQuestion, String lastAnswerSummary, Long productId, String contextType);

    long countSessions();

    int deleteExpired(LocalDateTime deadline);
}
