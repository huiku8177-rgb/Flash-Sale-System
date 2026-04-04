package com.flashsale.aiservice.service;

import com.flashsale.aiservice.domain.po.ChatRecordPO;

import java.util.List;

public interface ChatCacheService {

    List<ChatRecordPO> getRecentHistory(String sessionId);

    void cacheRecentHistory(String sessionId, List<ChatRecordPO> records);

    void evictSession(String sessionId);
}
