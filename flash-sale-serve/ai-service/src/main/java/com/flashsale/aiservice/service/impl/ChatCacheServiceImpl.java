package com.flashsale.aiservice.service.impl;

import com.flashsale.aiservice.config.AiProperties;
import com.flashsale.aiservice.domain.po.ChatRecordPO;
import com.flashsale.aiservice.service.ChatCacheService;
import com.flashsale.aiservice.util.ChatCacheKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class ChatCacheServiceImpl implements ChatCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ChatJsonCodec chatJsonCodec;
    private final AiProperties aiProperties;

    public ChatCacheServiceImpl(StringRedisTemplate stringRedisTemplate, ChatJsonCodec chatJsonCodec, AiProperties aiProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.chatJsonCodec = chatJsonCodec;
        this.aiProperties = aiProperties;
    }

    @Override
    public List<ChatRecordPO> getRecentHistory(String sessionId) {
        try {
            String value = stringRedisTemplate.opsForValue().get(ChatCacheKeys.sessionHistory(sessionId));
            return chatJsonCodec.readRecordList(value);
        } catch (Exception ex) {
            log.warn("Failed to load chat history from Redis, sessionId={}", sessionId, ex);
            return List.of();
        }
    }

    @Override
    public void cacheRecentHistory(String sessionId, List<ChatRecordPO> records) {
        try {
            String key = ChatCacheKeys.sessionHistory(sessionId);
            String value = chatJsonCodec.writeRecordList(records);
            stringRedisTemplate.opsForValue().set(key, value, Duration.ofDays(aiProperties.getSessionTtlDays()));
        } catch (Exception ex) {
            log.warn("Failed to cache chat history to Redis, sessionId={}", sessionId, ex);
        }
    }

    @Override
    public void evictSession(String sessionId) {
        try {
            stringRedisTemplate.delete(ChatCacheKeys.sessionHistory(sessionId));
        } catch (Exception ex) {
            log.warn("Failed to evict chat history from Redis, sessionId={}", sessionId, ex);
        }
    }
}
