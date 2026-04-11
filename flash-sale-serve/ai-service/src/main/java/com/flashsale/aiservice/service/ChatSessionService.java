package com.flashsale.aiservice.service;

import com.flashsale.aiservice.domain.po.ChatSessionPO;
import com.flashsale.aiservice.domain.vo.ChatSessionSummaryVO;
import com.flashsale.aiservice.domain.vo.ConversationContextState;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatSessionService {

    ChatSessionPO getOrCreate(String sessionId, Long userId, Long productId, String contextType);

    ChatSessionPO getRequired(String sessionId, Long userId);

    ConversationContextState getContextState(ChatSessionPO session);

    void refreshSession(ChatSessionPO session, String lastQuestion, String lastAnswerSummary,
                        Long productId, String contextType, ConversationContextState contextState);

    List<ChatSessionSummaryVO> listByUserId(Long userId, int limit);

    void deleteSession(String sessionId, Long userId);

    long countSessions();

    int deleteExpired(LocalDateTime deadline);
}
