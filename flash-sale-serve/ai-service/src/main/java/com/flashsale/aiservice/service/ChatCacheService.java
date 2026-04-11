package com.flashsale.aiservice.service;

import com.flashsale.aiservice.domain.po.ChatRecordPO;
import com.flashsale.aiservice.domain.vo.ConversationContextState;

import java.util.List;

public interface ChatCacheService {

    List<ChatRecordPO> getRecentHistory(String sessionId);

    void cacheRecentHistory(String sessionId, List<ChatRecordPO> records);

    ConversationContextState getContextState(String sessionId);

    void cacheContextState(String sessionId, ConversationContextState contextState);

    void evictSession(String sessionId);
}
