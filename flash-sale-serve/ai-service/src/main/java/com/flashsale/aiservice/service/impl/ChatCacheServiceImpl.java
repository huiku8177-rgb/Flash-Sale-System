package com.flashsale.aiservice.service.impl;

import com.flashsale.aiservice.config.AiProperties;
import com.flashsale.aiservice.domain.po.ChatRecordPO;
import com.flashsale.aiservice.domain.vo.ConversationContextState;
import com.flashsale.aiservice.service.ChatCacheService;
import com.flashsale.aiservice.util.ChatCacheKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;

/**
 * 对话缓存服务 Redis 实现类
 *
 * 负责将对话会话的热点数据（近期历史记录、上下文状态）缓存到 Redis 中，
 * 以减少对数据库的频繁读取，提升多轮对话的响应速度。
 *
 * 缓存策略：
 * - 缓存 Key 通过 ChatCacheKeys 工具类统一生成。
 * - 缓存有效期与会话 TTL 保持一致，由 AiProperties 配置。
 * - 采用降级容错设计：Redis 读写异常仅记录日志，不影响主业务流程。
 */
@Service
@Slf4j
public class ChatCacheServiceImpl implements ChatCacheService {

  private final StringRedisTemplate stringRedisTemplate;  // Redis 操作模板
  private final ChatJsonCodec chatJsonCodec;              // JSON 编解码工具
  private final AiProperties aiProperties;                // AI 配置属性（含 TTL）

  public ChatCacheServiceImpl(StringRedisTemplate stringRedisTemplate,
                              ChatJsonCodec chatJsonCodec,
                              AiProperties aiProperties) {
    this.stringRedisTemplate = stringRedisTemplate;
    this.chatJsonCodec = chatJsonCodec;
    this.aiProperties = aiProperties;
  }

  /**
   * 从 Redis 获取会话的近期对话历史
   *
   * 若 Redis 读取失败或缓存不存在，返回空列表（降级处理）。
   *
   * @param sessionId 会话ID
   * @return 近期对话记录列表，若无缓存则返回空列表
   */
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

  /**
   * 将近期对话历史写入 Redis 缓存
   *
   * 缓存有效期与会话 TTL 一致。
   * 写入失败仅记录日志，不影响主流程。
   *
   * @param sessionId 会话ID
   * @param records   待缓存的对话记录列表
   */
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

  /**
   * 从 Redis 获取会话的上下文状态
   *
   * 若读取失败，返回一个新的空上下文状态对象（降级处理）。
   *
   * @param sessionId 会话ID
   * @return 上下文状态，若无缓存则返回新实例
   */
  @Override
  public ConversationContextState getContextState(String sessionId) {
    try {
      String value = stringRedisTemplate.opsForValue().get(ChatCacheKeys.sessionContext(sessionId));
      if (!StringUtils.hasText(value)) {
        return null;
      }
      return chatJsonCodec.readConversationContext(value);
    } catch (Exception ex) {
      log.warn("Failed to load chat context from Redis, sessionId={}", sessionId, ex);
      return null;
    }
  }

  /**
   * 将上下文状态写入 Redis 缓存
   *
   * 缓存有效期与会话 TTL 一致。
   * 写入失败仅记录日志。
   *
   * @param sessionId    会话ID
   * @param contextState 待缓存的上下文状态
   */
  @Override
  public void cacheContextState(String sessionId, ConversationContextState contextState) {
    try {
      String key = ChatCacheKeys.sessionContext(sessionId);
      String value = chatJsonCodec.writeConversationContext(contextState);
      stringRedisTemplate.opsForValue().set(key, value, Duration.ofDays(aiProperties.getSessionTtlDays()));
    } catch (Exception ex) {
      log.warn("Failed to cache chat context to Redis, sessionId={}", sessionId, ex);
    }
  }

  /**
   * 清除会话相关的所有 Redis 缓存
   *
   * 在会话删除时调用，同时删除历史记录缓存和上下文状态缓存。
   * 删除失败仅记录日志。
   *
   * @param sessionId 会话ID
   */
  @Override
  public void evictSession(String sessionId) {
    try {
      stringRedisTemplate.delete(List.of(
        ChatCacheKeys.sessionHistory(sessionId),
        ChatCacheKeys.sessionContext(sessionId)
      ));
    } catch (Exception ex) {
      log.warn("Failed to evict chat history from Redis, sessionId={}", sessionId, ex);
    }
  }
}
