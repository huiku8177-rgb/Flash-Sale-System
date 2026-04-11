package com.flashsale.aiservice.service.impl;

import com.flashsale.aiservice.client.ProductKnowledgeClient;
import com.flashsale.aiservice.client.SeckillKnowledgeClient;
import com.flashsale.aiservice.config.AiProperties;
import com.flashsale.aiservice.domain.dto.ProductKnowledgeDTO;
import com.flashsale.aiservice.domain.dto.SeckillKnowledgeDTO;
import com.flashsale.aiservice.domain.po.ChatSessionPO;
import com.flashsale.aiservice.domain.vo.ChatSessionSummaryVO;
import com.flashsale.aiservice.domain.vo.ConversationContextState;
import com.flashsale.aiservice.exception.AiServiceException;
import com.flashsale.aiservice.mapper.ChatSessionMapper;
import com.flashsale.aiservice.service.ChatCacheService;
import com.flashsale.aiservice.service.ChatSessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 会话管理服务实现类
 *
 * 负责对话会话的全生命周期管理，包括：
 * - 会话的创建、查询、刷新和删除
 * - 上下文状态（ConversationContextState）的持久化与缓存管理
 * - 上下文自动修复：补齐缺失的商品名称、类型等元数据
 * - 会话列表查询与过期清理
 */
@Service
public class ChatSessionServiceImpl implements ChatSessionService {

  private static final String PRODUCT_TYPE_NORMAL = "normal";
  private static final String PRODUCT_TYPE_SECKILL = "seckill";

  private final ChatSessionMapper chatSessionMapper;               // 会话数据访问层
  private final AiProperties aiProperties;                         // 配置属性（TTL等）
  private final ChatCacheService chatCacheService;                 // 会话缓存服务
  private final ChatJsonCodec chatJsonCodec;                       // JSON 编解码工具
  private final ProductKnowledgeClient productKnowledgeClient;     // 普通商品客户端
  private final SeckillKnowledgeClient seckillKnowledgeClient;     // 秒杀商品客户端

  public ChatSessionServiceImpl(ChatSessionMapper chatSessionMapper,
                                AiProperties aiProperties,
                                ChatCacheService chatCacheService,
                                ChatJsonCodec chatJsonCodec,
                                ProductKnowledgeClient productKnowledgeClient,
                                SeckillKnowledgeClient seckillKnowledgeClient) {
    this.chatSessionMapper = chatSessionMapper;
    this.aiProperties = aiProperties;
    this.chatCacheService = chatCacheService;
    this.chatJsonCodec = chatJsonCodec;
    this.productKnowledgeClient = productKnowledgeClient;
    this.seckillKnowledgeClient = seckillKnowledgeClient;
  }

  /**
   * 获取或创建会话
   *
   * 若传入的 sessionId 为空，则生成一个新的 UUID 作为会话ID。
   * 若会话已存在，校验用户权限（userId 匹配）。
   * 若会话不存在，创建新会话并初始化上下文状态。
   *
   * @param sessionId   客户端传入的会话ID（可为空）
   * @param userId      用户ID
   * @param productId   当前商品ID
   * @param contextType 上下文类型
   * @return 会话持久化对象
   * @throws AiServiceException 若会话不属于当前用户
   */
  @Override
  @Transactional
  public ChatSessionPO getOrCreate(String sessionId, Long userId, Long productId, String contextType) {
    String actualSessionId = (sessionId == null || sessionId.isBlank()) ? UUID.randomUUID().toString() : sessionId;
    ChatSessionPO existing = chatSessionMapper.getBySessionId(actualSessionId);
    if (existing != null) {
      // 校验用户权限：若会话已绑定用户且与当前用户不匹配，则拒绝访问
      if (userId != null && existing.getUserId() != null && !userId.equals(existing.getUserId())) {
        throw new AiServiceException("Session does not belong to current user");
      }
      return existing;
    }

    LocalDateTime now = LocalDateTime.now();
    ConversationContextState initialContext = new ConversationContextState();
    // 在初始上下文中保留商品ID，以便旧会话在首次读取时能够修复名称/类型
    initialContext.setCurrentProductId(productId);

    ChatSessionPO session = new ChatSessionPO();
    session.setSessionId(actualSessionId);
    session.setUserId(userId);
    session.setProductId(productId);
    session.setContextType(contextType);
    session.setSessionStatus(1);
    session.setMessageCount(0);
    session.setContextStateJson(chatJsonCodec.writeConversationContext(initialContext));
    session.setCreatedAt(now);
    session.setLastActiveAt(now);
    session.setExpireAt(now.plusDays(aiProperties.getSessionTtlDays()));
    chatSessionMapper.insert(session);
    return session;
  }

  /**
   * 获取会话（必须存在）
   *
   * @param sessionId 会话ID
   * @param userId    用户ID（用于权限校验）
   * @return 会话持久化对象
   * @throws AiServiceException 若会话不存在或不属于当前用户
   */
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

  /**
   * 获取会话的上下文状态
   *
   * 优先从缓存读取，若缓存未命中则从数据库 JSON 字段反序列化。
   * 读取后会自动修复缺失的商品元数据（名称、类型），并回写缓存和数据库。
   *
   * @param session 会话对象
   * @return 上下文状态
   */
  @Override
  @Transactional
  public ConversationContextState getContextState(ChatSessionPO session) {
    // 尝试从缓存获取
    ConversationContextState cached = chatCacheService.getContextState(session.getSessionId());
    if (cached != null) {
      boolean repaired = repairProductContext(session, cached);
      if (repaired) {
        persistContextRepair(session, cached);
      }
      chatCacheService.cacheContextState(session.getSessionId(), cached);
      return cached;
    }

    // 从数据库 JSON 字段反序列化
    ConversationContextState state = chatJsonCodec.readConversationContext(session.getContextStateJson());
    boolean repaired = repairProductContext(session, state);
    if (repaired) {
      persistContextRepair(session, state);
    }
    chatCacheService.cacheContextState(session.getSessionId(), state);
    return state;
  }

  /**
   * 刷新会话状态（每次对话后调用）
   *
   * 更新消息计数、最后提问、回答摘要、上下文状态、活跃时间、过期时间等。
   * 同时将更新后的上下文状态写入缓存。
   *
   * @param session          会话对象
   * @param lastQuestion     最后提问内容
   * @param lastAnswerSummary 回答摘要
   * @param productId        当前商品ID
   * @param contextType      上下文类型
   * @param contextState     新的上下文状态
   */
  @Override
  @Transactional
  public void refreshSession(ChatSessionPO session, String lastQuestion, String lastAnswerSummary,
                             Long productId, String contextType, ConversationContextState contextState) {
    Long effectiveProductId = productId != null ? productId : contextState.getCurrentProductId();

    session.setMessageCount(session.getMessageCount() == null ? 1 : session.getMessageCount() + 1);
    session.setLastQuestion(lastQuestion);
    session.setLastAnswerSummary(lastAnswerSummary);
    session.setContextStateJson(chatJsonCodec.writeConversationContext(contextState));
    session.setLastActiveAt(LocalDateTime.now());
    session.setExpireAt(LocalDateTime.now().plusDays(aiProperties.getSessionTtlDays()));
    // 若会话级别尚未记录商品ID，则用当前有效的商品ID回填
    if (session.getProductId() == null && effectiveProductId != null) {
      session.setProductId(effectiveProductId);
    }
    if ((session.getContextType() == null || session.getContextType().isBlank()) && contextType != null) {
      session.setContextType(contextType);
    }
    chatSessionMapper.updateActivity(session);
    chatCacheService.cacheContextState(session.getSessionId(), contextState);
  }

  /**
   * 按用户ID查询会话摘要列表
   *
   * @param userId 用户ID
   * @param limit  返回数量上限（1-100，默认20）
   * @return 会话摘要列表
   */
  @Override
  public List<ChatSessionSummaryVO> listByUserId(Long userId, int limit) {
    if (userId == null) {
      throw new AiServiceException("User id is required");
    }
    int actualLimit = limit <= 0 ? 20 : Math.min(limit, 100);
    return chatSessionMapper.listByUserId(userId, actualLimit).stream()
      .map(this::toSummary)
      .toList();
  }

  /**
   * 删除会话（软删除）
   *
   * 校验用户权限，清除缓存，然后从数据库删除。
   *
   * @param sessionId 会话ID
   * @param userId    用户ID
   */
  @Override
  @Transactional
  public void deleteSession(String sessionId, Long userId) {
    ChatSessionPO session = getRequired(sessionId, userId);
    chatCacheService.evictSession(session.getSessionId());
    chatSessionMapper.deleteBySessionId(session.getSessionId());
  }

  /**
   * 统计总会话数
   */
  @Override
  public long countSessions() {
    return chatSessionMapper.countSessions();
  }

  /**
   * 批量删除过期的会话
   *
   * @param deadline 过期时间阈值
   * @return 删除的会话数量
   */
  @Override
  @Transactional
  public int deleteExpired(LocalDateTime deadline) {
    return chatSessionMapper.deleteExpired(deadline);
  }

  /**
   * 将会话PO转换为摘要VO
   */
  private ChatSessionSummaryVO toSummary(ChatSessionPO session) {
    ConversationContextState state = chatJsonCodec.readConversationContext(session.getContextStateJson());
    ChatSessionSummaryVO summary = new ChatSessionSummaryVO();
    summary.setSessionId(session.getSessionId());
    summary.setUserId(session.getUserId());
    summary.setProductId(session.getProductId());
    summary.setContextType(session.getContextType());
    summary.setMessageCount(session.getMessageCount());
    summary.setLastQuestion(session.getLastQuestion());
    summary.setLastAnswerSummary(session.getLastAnswerSummary());
    summary.setCurrentProductName(state.getCurrentProductName());
    summary.setCurrentIntentType(state.getCurrentIntentType());
    summary.setCreatedAt(session.getCreatedAt());
    summary.setLastActiveAt(session.getLastActiveAt());
    return summary;
  }

  /**
   * 修复会话上下文中的商品元数据（名称、类型）
   *
   * 用于兼容旧版本数据（仅存储了商品ID）或缓存穿透后的恢复。
   * 修复策略：
   * - 初始化集合字段（防止NPE）
   * - 若上下文中无商品ID，尝试从会话级别获取
   * - 若已有商品名称和类型，跳过修复
   * - 否则调用商品知识库客户端获取完整信息并回填
   *
   * @param session 会话对象
   * @param state   上下文状态
   * @return 是否进行了任何修改
   */
  private boolean repairProductContext(ChatSessionPO session, ConversationContextState state) {
    if (state == null) {
      return false;
    }

    boolean changed = false;
    // 确保集合字段非空
    if (state.getCompareCandidateIds() == null) {
      state.setCompareCandidateIds(List.of());
      changed = true;
    }
    if (state.getCompareCandidateNames() == null) {
      state.setCompareCandidateNames(List.of());
      changed = true;
    }

    Long effectiveProductId = state.getCurrentProductId() != null ? state.getCurrentProductId() : session.getProductId();
    if (effectiveProductId == null) {
      return changed;
    }

    if (state.getCurrentProductId() == null) {
      state.setCurrentProductId(effectiveProductId);
      changed = true;
    }

    // 修复仅保留了商品ID的旧上下文，使后续的代词改写能够正常工作
    if (StringUtils.hasText(state.getCurrentProductName()) && StringUtils.hasText(state.getCurrentProductType())) {
      return changed;
    }

    // 优先使用上下文中已有的商品名称/类型，若不存在则按默认顺序查询商品库
    ProductIdentity identity = fetchProductIdentity(effectiveProductId, state.getCurrentProductType(), state.getCurrentProductName());
    if (identity == null) {
      return changed;
    }

    if (!StringUtils.hasText(state.getCurrentProductName())) {
      state.setCurrentProductName(identity.productName());
      changed = true;
    }
    if (!StringUtils.hasText(state.getCurrentProductType())) {
      state.setCurrentProductType(identity.productType());
      changed = true;
    }
    return changed;
  }

  /**
   * 将修复后的上下文状态持久化到数据库
   */
  private void persistContextRepair(ChatSessionPO session, ConversationContextState state) {
    session.setContextStateJson(chatJsonCodec.writeConversationContext(state));
    if (session.getProductId() == null && state.getCurrentProductId() != null) {
      session.setProductId(state.getCurrentProductId());
    }
    chatSessionMapper.updateActivity(session);
  }

  /**
   * 获取商品身份信息（名称和类型）
   *
   * 查询策略：
   * 1. 若指定了首选类型，优先按该类型查询
   * 2. 若调用方已知商品名称，利用名称辅助区分同一ID可能对应的两种商品类型
   * 3. 默认返回普通商品，若不存在则返回秒杀商品
   *
   * @param productId     商品ID
   * @param preferredType 首选类型（可为空）
   * @param preferredName 已知的商品名称（可为空）
   * @return 商品身份信息，若找不到则返回 null
   */
  private ProductIdentity fetchProductIdentity(Long productId, String preferredType, String preferredName) {
    if (productId == null) {
      return null;
    }

    ProductIdentity normalIdentity = null;
    ProductIdentity seckillIdentity = null;

    // 优先按首选类型查询
    if (PRODUCT_TYPE_SECKILL.equalsIgnoreCase(preferredType)) {
      seckillIdentity = fetchSeckillIdentity(productId);
      if (seckillIdentity != null) {
        return seckillIdentity;
      }
    }
    if (PRODUCT_TYPE_NORMAL.equalsIgnoreCase(preferredType)) {
      normalIdentity = fetchNormalIdentity(productId);
      if (normalIdentity != null) {
        return normalIdentity;
      }
    }

    // 若调用方已知商品名称但未知类型，先按名称匹配，避免默认将重复ID解析为普通商品
    if (StringUtils.hasText(preferredName)) {
      if (normalIdentity == null) {
        normalIdentity = fetchNormalIdentity(productId);
      }
      if (seckillIdentity == null) {
        seckillIdentity = fetchSeckillIdentity(productId);
      }
      if (matchesProductName(normalIdentity, preferredName)) {
        return normalIdentity;
      }
      if (matchesProductName(seckillIdentity, preferredName)) {
        return seckillIdentity;
      }
    }

    if (normalIdentity == null) {
      normalIdentity = fetchNormalIdentity(productId);
    }
    if (normalIdentity != null) {
      return normalIdentity;
    }
    if (seckillIdentity == null) {
      seckillIdentity = fetchSeckillIdentity(productId);
    }
    return seckillIdentity;
  }

  /**
   * 从普通商品库获取商品身份信息
   */
  private ProductIdentity fetchNormalIdentity(Long productId) {
    ProductKnowledgeDTO product = productKnowledgeClient.getProductById(productId);
    if (product == null || !StringUtils.hasText(product.getName())) {
      return null;
    }
    return new ProductIdentity(product.getName(), PRODUCT_TYPE_NORMAL);
  }

  /**
   * 从秒杀商品库获取商品身份信息
   */
  private ProductIdentity fetchSeckillIdentity(Long productId) {
    SeckillKnowledgeDTO seckill = seckillKnowledgeClient.getProductById(productId);
    if (seckill == null || !StringUtils.hasText(seckill.getName())) {
      return null;
    }
    return new ProductIdentity(seckill.getName(), PRODUCT_TYPE_SECKILL);
  }

  /**
   * 检查商品名称是否匹配（忽略大小写和空格）
   */
  private boolean matchesProductName(ProductIdentity identity, String preferredName) {
    if (identity == null || !StringUtils.hasText(preferredName)) {
      return false;
    }
    return normalizeProductName(identity.productName()).equals(normalizeProductName(preferredName));
  }

  /**
   * 规范化商品名称：转小写、移除所有空白字符
   */
  private String normalizeProductName(String productName) {
    if (!StringUtils.hasText(productName)) {
      return "";
    }
    return productName.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
  }

  /**
   * 商品身份信息记录（内部使用）
   */
  private record ProductIdentity(String productName, String productType) {
  }
}
