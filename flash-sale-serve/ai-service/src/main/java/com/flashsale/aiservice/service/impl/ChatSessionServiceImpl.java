package com.flashsale.aiservice.service.impl;

import com.flashsale.aiservice.config.AiProperties;
import com.flashsale.aiservice.domain.po.ChatSessionPO;
import com.flashsale.aiservice.exception.AiServiceException;
import com.flashsale.aiservice.mapper.ChatSessionMapper;
import com.flashsale.aiservice.service.ChatSessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ChatSessionServiceImpl implements ChatSessionService {

    private final ChatSessionMapper chatSessionMapper;
    private final AiProperties aiProperties;

    public ChatSessionServiceImpl(ChatSessionMapper chatSessionMapper, AiProperties aiProperties) {
        this.chatSessionMapper = chatSessionMapper;
        this.aiProperties = aiProperties;
    }

    @Override
    @Transactional
    public ChatSessionPO getOrCreate(String sessionId, Long userId, Long productId, String contextType) {
        String actualSessionId = (sessionId == null || sessionId.isBlank()) ? UUID.randomUUID().toString() : sessionId;
        ChatSessionPO existing = chatSessionMapper.getBySessionId(actualSessionId);
        if (existing != null) {
            if (userId != null && existing.getUserId() != null && !userId.equals(existing.getUserId())) {
                throw new AiServiceException("Session does not belong to current user");
            }
            return existing;
        }

        // V1 stores session metadata in MySQL and lets Redis only cache recent turns.
        LocalDateTime now = LocalDateTime.now();
        ChatSessionPO session = new ChatSessionPO();
        session.setSessionId(actualSessionId);
        session.setUserId(userId);
        session.setProductId(productId);
        session.setContextType(contextType);
        session.setSessionStatus(1);
        session.setMessageCount(0);
        session.setCreatedAt(now);
        session.setLastActiveAt(now);
        session.setExpireAt(now.plusDays(aiProperties.getSessionTtlDays()));
        chatSessionMapper.insert(session);
        return session;
    }

    @Override
    public ChatSessionPO getRequired(String sessionId, Long userId) {
        ChatSessionPO session = chatSessionMapper.getBySessionId(sessionId);
        if (session == null) {
            throw new AiServiceException("Chat session not found: " + sessionId);
        }
        if (userId != null && session.getUserId() != null && !userId.equals(session.getUserId())) {
            throw new AiServiceException("Session does not belong to current user");
        }
        return session;
    }

    @Override
    @Transactional
    public void refreshSession(ChatSessionPO session, String lastQuestion, String lastAnswerSummary, Long productId, String contextType) {
        session.setMessageCount(session.getMessageCount() == null ? 1 : session.getMessageCount() + 1);
        session.setLastQuestion(lastQuestion);
        session.setLastAnswerSummary(lastAnswerSummary);
        session.setLastActiveAt(LocalDateTime.now());
        session.setExpireAt(LocalDateTime.now().plusDays(aiProperties.getSessionTtlDays()));
        if (session.getProductId() == null && productId != null) {
            session.setProductId(productId);
        }
        if ((session.getContextType() == null || session.getContextType().isBlank()) && contextType != null) {
            session.setContextType(contextType);
        }
        chatSessionMapper.updateActivity(session);
    }

    @Override
    public long countSessions() {
        return chatSessionMapper.countSessions();
    }

    @Override
    @Transactional
    public int deleteExpired(LocalDateTime deadline) {
        return chatSessionMapper.deleteExpired(deadline);
    }
}
