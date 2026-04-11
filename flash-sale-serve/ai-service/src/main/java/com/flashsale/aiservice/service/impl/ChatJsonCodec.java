package com.flashsale.aiservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.aiservice.domain.po.ChatRecordPO;
import com.flashsale.aiservice.domain.vo.ConversationContextState;
import com.flashsale.aiservice.domain.vo.ProductCandidateVO;
import com.flashsale.aiservice.domain.vo.RelatedKnowledgeVO;
import com.flashsale.aiservice.exception.AiServiceException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 对话 JSON 编解码工具类
 *
 * 负责将对话相关的复杂 Java 对象序列化为 JSON 字符串（用于数据库存储），
 * 以及将 JSON 字符串反序列化回强类型的 Java 对象。
 *
 * 设计要点：
 * - 使用 Jackson ObjectMapper 作为底层序列化引擎。
 * - 通过 TypeReference 保留泛型信息，确保反序列化类型安全。
 * - 提供针对常用数据类型的便捷方法，减少上层代码的重复。
 * - 对空值/空字符串进行特殊处理，返回默认空集合而非 null，避免 NPE。
 * - 将受检异常 JsonProcessingException 包装为运行时异常 AiServiceException。
 */
@Component
public class ChatJsonCodec {

  // 泛型类型引用常量，用于反序列化时保留完整的泛型信息
  private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};
  private static final TypeReference<List<Long>> LONG_LIST_TYPE = new TypeReference<>() {};
  private static final TypeReference<List<RelatedKnowledgeVO>> KNOWLEDGE_LIST_TYPE = new TypeReference<>() {};
  private static final TypeReference<List<ChatRecordPO>> CHAT_RECORD_LIST_TYPE = new TypeReference<>() {};
  private static final TypeReference<List<ProductCandidateVO>> PRODUCT_CANDIDATE_LIST_TYPE = new TypeReference<>() {};
  private static final TypeReference<ConversationContextState> CONTEXT_STATE_TYPE = new TypeReference<>() {};

  private final ObjectMapper objectMapper;

  public ChatJsonCodec(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  // ==================== 序列化方法（对象 → JSON 字符串） ====================

  public String writeStringList(List<String> values) {
    return writeValue(values);
  }

  public String writeKnowledgeList(List<RelatedKnowledgeVO> values) {
    return writeValue(values);
  }

  public String writeRecordList(List<ChatRecordPO> values) {
    return writeValue(values);
  }

  public String writeLongList(List<Long> values) {
    return writeValue(values);
  }

  public String writeCandidateList(List<ProductCandidateVO> values) {
    return writeValue(values);
  }

  public String writeConversationContext(ConversationContextState value) {
    return writeValue(value);
  }

  // ==================== 反序列化方法（JSON 字符串 → 对象） ====================

  public List<String> readStringList(String value) {
    return readValue(value, STRING_LIST_TYPE);
  }

  public List<RelatedKnowledgeVO> readKnowledgeList(String value) {
    return readValue(value, KNOWLEDGE_LIST_TYPE);
  }

  public List<ChatRecordPO> readRecordList(String value) {
    return readValue(value, CHAT_RECORD_LIST_TYPE);
  }

  public List<Long> readLongList(String value) {
    return readValue(value, LONG_LIST_TYPE);
  }

  public List<ProductCandidateVO> readCandidateList(String value) {
    return readValue(value, PRODUCT_CANDIDATE_LIST_TYPE);
  }

  public ConversationContextState readConversationContext(String value) {
    return readValue(value, CONTEXT_STATE_TYPE);
  }

  /**
   * 通用的对象序列化方法
   *
   * @param value 待序列化的对象
   * @return JSON 字符串
   * @throws AiServiceException 序列化失败时抛出
   */
  private String writeValue(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      throw new AiServiceException("Failed to serialize chat payload", ex);
    }
  }

  /**
   * 通用的对象反序列化方法
   *
   * 若传入的 JSON 字符串为空或空白，则返回对应类型的默认空值。
   *
   * @param value         JSON 字符串
   * @param typeReference 目标类型的泛型引用
   * @return 反序列化后的对象
   * @throws AiServiceException 反序列化失败时抛出
   */
  private <T> T readValue(String value, TypeReference<T> typeReference) {
    if (value == null || value.isBlank()) {
      return readEmpty(typeReference);
    }
    try {
      return objectMapper.readValue(value, typeReference);
    } catch (JsonProcessingException ex) {
      throw new AiServiceException("Failed to deserialize chat payload", ex);
    }
  }

  /**
   * 返回各类型对应的默认空值
   *
   * 集合类型返回空 List，ConversationContextState 返回新实例。
   *
   * @param typeReference 类型引用
   * @return 默认空值
   */
  @SuppressWarnings("unchecked")
  private <T> T readEmpty(TypeReference<T> typeReference) {
    if (typeReference == STRING_LIST_TYPE
      || typeReference == LONG_LIST_TYPE
      || typeReference == KNOWLEDGE_LIST_TYPE
      || typeReference == CHAT_RECORD_LIST_TYPE
      || typeReference == PRODUCT_CANDIDATE_LIST_TYPE) {
      return (T) List.of();
    }
    if (typeReference == CONTEXT_STATE_TYPE) {
      return (T) new ConversationContextState();
    }
    return null;
  }
}
