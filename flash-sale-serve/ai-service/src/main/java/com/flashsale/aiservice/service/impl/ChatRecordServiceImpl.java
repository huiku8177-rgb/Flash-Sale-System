package com.flashsale.aiservice.service.impl;

import com.flashsale.aiservice.config.AiProperties;
import com.flashsale.aiservice.domain.po.ChatRecordPO;
import com.flashsale.aiservice.domain.po.ChatSessionPO;
import com.flashsale.aiservice.domain.vo.ChatRecordVO;
import com.flashsale.aiservice.domain.vo.ChatSessionVO;
import com.flashsale.aiservice.domain.vo.ConversationContextState;
import com.flashsale.aiservice.mapper.ChatRecordMapper;
import com.flashsale.aiservice.service.ChatCacheService;
import com.flashsale.aiservice.service.ChatRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 对话记录服务实现类
 *
 * 负责对话记录的持久化、缓存管理以及会话详情的组装。
 *
 * 设计要点：
 * - 每条记录分配一个会话内递增的 recordNo，用于排序。
 * - 保存记录后自动更新 Redis 缓存中的近期历史，加速多轮对话上下文获取。
 * - 查询历史时优先从缓存读取，缓存未命中则回查数据库并回填缓存。
 * - 支持按会话ID批量删除及过期记录清理。
 */
@Service
public class ChatRecordServiceImpl implements ChatRecordService {

  private final ChatRecordMapper chatRecordMapper;       // 对话记录数据访问层
  private final ChatCacheService chatCacheService;       // Redis 缓存服务
  private final ChatJsonCodec chatJsonCodec;             // JSON 编解码工具
  private final AiProperties aiProperties;               // AI 配置属性

  public ChatRecordServiceImpl(ChatRecordMapper chatRecordMapper, ChatCacheService chatCacheService,
                               ChatJsonCodec chatJsonCodec, AiProperties aiProperties) {
    this.chatRecordMapper = chatRecordMapper;
    this.chatCacheService = chatCacheService;
    this.chatJsonCodec = chatJsonCodec;
    this.aiProperties = aiProperties;
  }

  /**
   * 保存一条对话记录
   *
   * 流程：
   * 1. 计算当前会话的下一个 recordNo（自增序号）
   * 2. 将记录插入数据库
   * 3. 从数据库重新拉取该会话的近期历史记录（缓存数量由配置决定）
   * 4. 按 recordNo 排序后更新到 Redis 缓存
   *
   * @param record 待保存的对话记录
   * @return 保存后的记录对象（含生成的 recordNo）
   */
  @Override
  @Transactional
  public ChatRecordPO save(ChatRecordPO record) {
    // 获取会话内下一个记录序号，若为空则从1开始
    Integer nextRecordNo = chatRecordMapper.nextRecordNo(record.getSessionId());
    record.setRecordNo(nextRecordNo == null ? 1 : nextRecordNo);
    chatRecordMapper.insert(record);

    // 从数据库拉取最新的近期历史，用于更新缓存
    List<ChatRecordPO> recentHistory = chatRecordMapper.listRecentBySessionId(
      record.getSessionId(),
      aiProperties.getHistoryCacheSize()
    );
    List<ChatRecordPO> normalizedRecords = new ArrayList<>(recentHistory);
    // 确保按序号排序，便于多轮对话按时间顺序展示
    normalizedRecords.sort(Comparator.comparing(ChatRecordPO::getRecordNo));
    chatCacheService.cacheRecentHistory(record.getSessionId(), normalizedRecords);
    return record;
  }

  /**
   * 获取指定会话的近期对话历史
   *
   * 优先从 Redis 缓存读取，若缓存为空或不足则查询数据库。
   * 返回的记录按 recordNo 升序排列，且数量不超过 limit。
   *
   * @param sessionId 会话ID
   * @param limit     最大返回条数
   * @return 近期对话记录列表（按时间顺序）
   */
  @Override
  public List<ChatRecordPO> listRecentHistory(String sessionId, int limit) {
    List<ChatRecordPO> cachedRecords = chatCacheService.getRecentHistory(sessionId);
    if (!cachedRecords.isEmpty()) {
      // 缓存命中：按序号排序，并截取最后 limit 条
      List<ChatRecordPO> sortedRecords = cachedRecords.stream()
        .sorted(Comparator.comparing(ChatRecordPO::getRecordNo))
        .toList();
      if (sortedRecords.size() <= limit) {
        return sortedRecords;
      }
      return sortedRecords.subList(sortedRecords.size() - limit, sortedRecords.size());
    }

    // 缓存未命中：从数据库查询
    List<ChatRecordPO> records = chatRecordMapper.listRecentBySessionId(sessionId, limit);
    List<ChatRecordPO> normalizedRecords = new ArrayList<>(records);
    normalizedRecords.sort(Comparator.comparing(ChatRecordPO::getRecordNo));
    if (!normalizedRecords.isEmpty()) {
      // 回填缓存，加速后续访问
      chatCacheService.cacheRecentHistory(sessionId, normalizedRecords);
    }
    return normalizedRecords;
  }

  /**
   * 获取会话详情（包含对话记录列表和上下文状态）
   *
   * @param session 会话持久化对象
   * @param limit   对话记录返回数量上限
   * @return 会话详情 VO
   */
  @Override
  public ChatSessionVO getSessionDetail(ChatSessionPO session, int limit) {
    List<ChatRecordPO> records = chatRecordMapper.listBySessionId(session.getSessionId(), limit);
    ChatSessionVO sessionVO = new ChatSessionVO();
    sessionVO.setSessionId(session.getSessionId());
    sessionVO.setUserId(session.getUserId());
    sessionVO.setProductId(session.getProductId());
    sessionVO.setContextType(session.getContextType());
    sessionVO.setMessageCount(session.getMessageCount());
    sessionVO.setCreatedAt(session.getCreatedAt());
    sessionVO.setLastActiveAt(session.getLastActiveAt());
    // 反序列化上下文状态 JSON
    sessionVO.setContextState(chatJsonCodec.readConversationContext(session.getContextStateJson()));
    // 转换记录列表
    sessionVO.setRecords(records.stream().map(this::toChatRecordVO).toList());
    return sessionVO;
  }

  /**
   * 按会话 ID 删除所有对话记录
   *
   * 同时清除该会话的 Redis 缓存。
   *
   * @param sessionId 会话ID
   */
  @Override
  @Transactional
  public void deleteBySessionId(String sessionId) {
    chatRecordMapper.deleteBySessionId(sessionId);
    chatCacheService.evictSession(sessionId);
  }

  /**
   * 统计总对话记录数
   */
  @Override
  public long countRecords() {
    return chatRecordMapper.countRecords();
  }

  /**
   * 批量删除过期的对话记录
   *
   * @param deadline 过期时间阈值
   * @return 删除的记录数量
   */
  @Override
  @Transactional
  public int deleteExpired(LocalDateTime deadline) {
    return chatRecordMapper.deleteExpired(deadline);
  }

  /**
   * 将持久化对象转换为视图对象
   *
   * 包含 JSON 字段的反序列化（来源列表、知识证据、对比候选商品等）
   */
  private ChatRecordVO toChatRecordVO(ChatRecordPO record) {
    ChatRecordVO vo = new ChatRecordVO();
    vo.setRecordNo(record.getRecordNo());
    vo.setQuestion(record.getQuestion());
    vo.setQuestionCategory(record.getQuestionCategory());
    vo.setIntentType(record.getIntentType());
    vo.setRouteType(record.getRouteType());
    vo.setRewrittenQuestion(record.getRewrittenQuestion());
    vo.setAnswer(record.getAnswer());
    vo.setAnswerPolicy(record.getAnswerPolicy());
    vo.setSources(chatJsonCodec.readStringList(record.getSourcesJson()));
    vo.setHitKnowledge(chatJsonCodec.readKnowledgeList(record.getHitKnowledgeJson()));
    vo.setCompareCandidates(chatJsonCodec.readCandidateList(record.getCompareCandidatesJson()));
    vo.setConfidence(record.getConfidence());
    vo.setFallbackReason(record.getFallbackReason());
    vo.setAuditSummary(record.getAuditSummary());
    vo.setCreatedAt(record.getCreatedAt());
    return vo;
  }
}
