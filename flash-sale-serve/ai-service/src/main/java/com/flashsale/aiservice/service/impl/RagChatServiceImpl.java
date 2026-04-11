
  package com.flashsale.aiservice.service.impl;

import com.flashsale.aiservice.client.ProductKnowledgeClient;
import com.flashsale.aiservice.client.SeckillKnowledgeClient;
import com.flashsale.aiservice.config.AiProperties;
import com.flashsale.aiservice.domain.dto.ChatRequestDTO;
import com.flashsale.aiservice.domain.dto.ProductKnowledgeDTO;
import com.flashsale.aiservice.domain.dto.SeckillKnowledgeDTO;
import com.flashsale.aiservice.domain.enums.OutOfScopeTopicType;
import com.flashsale.aiservice.domain.enums.QuestionCategory;
import com.flashsale.aiservice.domain.enums.QuestionIntentType;
import com.flashsale.aiservice.domain.po.ChatRecordPO;
import com.flashsale.aiservice.domain.po.ChatSessionPO;
import com.flashsale.aiservice.domain.vo.ChatResponseVO;
import com.flashsale.aiservice.domain.vo.ChatSessionSummaryVO;
import com.flashsale.aiservice.domain.vo.ChatSessionVO;
import com.flashsale.aiservice.domain.vo.ConversationContextState;
import com.flashsale.aiservice.service.ChatAuditService;
import com.flashsale.aiservice.service.ChatRecordService;
import com.flashsale.aiservice.service.ChatSessionService;
import com.flashsale.aiservice.service.RagChatService;
import com.flashsale.aiservice.service.route.ChatRouteRequest;
import com.flashsale.aiservice.service.route.ChatRouteResult;
import com.flashsale.aiservice.service.route.ChatRouteStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RAG 对话服务实现类
 *
 * 负责处理用户对话请求的完整流程，包括：
 * - 会话管理（创建、查询、删除）
 * - 意图识别与问题改写
 * - 上下文状态维护（当前商品、对比候选等）
 * - 路由策略分发与执行
 * - 对话记录持久化与审计
 *
 * 该类是 RAG 对话系统的核心入口，协调各个子服务完成智能问答。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagChatServiceImpl implements RagChatService {

  // 商品类型常量
  private static final String PRODUCT_TYPE_NORMAL = "normal";
  private static final String PRODUCT_TYPE_SECKILL = "seckill";

  // 越界话题关键词 - 天气
  private static final String QUESTION_WEATHER = "天气";
  // 越界话题关键词 - 金融投资
  private static final String QUESTION_STOCK = "股票";
  private static final String QUESTION_FUND = "基金";
  private static final String QUESTION_FINANCIAL = "理财";
  private static final String QUESTION_MARKET = "大盘";
  // 越界话题关键词 - 技术编程
  private static final String QUESTION_CODE = "代码";
  private static final String QUESTION_PROGRAM = "编程";
  private static final String QUESTION_BUG = "bug";
  // 越界话题关键词 - 政治新闻
  private static final String QUESTION_PRESIDENT = "总统";
  private static final String QUESTION_POLICY = "政策";
  private static final String QUESTION_NEWS = "新闻";
  // 越界话题关键词 - 医疗健康
  private static final String QUESTION_DOCTOR = "医生";
  private static final String QUESTION_HOSPITAL = "医院";
  private static final String QUESTION_MEDICINE = "药";
  // 越界话题关键词 - 法律咨询
  private static final String QUESTION_LAW = "法律";
  private static final String QUESTION_LAWYER = "律师";

  // 售后政策相关关键词
  private static final String TERM_REFUND = "退款";
  private static final String TERM_RETURN = "退货";
  private static final String TERM_AFTER_SALES = "售后";
  private static final String TERM_WARRANTY = "保修";

  // 配送政策相关关键词
  private static final String TERM_DELIVERY = "配送";
  private static final String TERM_SHIPPING = "发货";
  private static final String TERM_EXPRESS = "快递";
  private static final String TERM_LOGISTICS = "物流";
  private static final String TERM_FREIGHT = "运费";

  // 实时状态相关关键词
  private static final String TERM_INVENTORY = "库存";
  private static final String TERM_AVAILABLE = "有货";
  private static final String TERM_PRICE = "价格";
  private static final String TERM_HOW_MUCH = "多少钱";
  private static final String TERM_SECKILL_PRICE = "秒杀价";

  // 秒杀活动相关关键词
  private static final String TERM_SECKILL = "秒杀";
  private static final String TERM_ACTIVITY = "活动";
  private static final String TERM_START = "开始";
  private static final String TERM_END = "结束";

  // 商品指代与对比相关关键词
  private static final String TERM_PRODUCT = "商品";
  private static final String TERM_THIS_PRODUCT = "这款商品";
  private static final String TERM_THIS_ITEM = "这个商品";
  private static final String TERM_THIS = "这款";
  private static final String TERM_IT = "它";
  private static final String TERM_THAT = "那款";
  private static final String TERM_COMPARE = "同类";
  private static final String TERM_COMPARE2 = "比较";
  private static final String TERM_COMPARE3 = "对比";
  private static final String TERM_ADVANTAGE = "优势";
  private static final String TERM_DISADVANTAGE = "不足";
  private static final String TERM_RECOMMEND = "推荐";
  private static final String TERM_WORTH_BUYING = "值得买";
  private static final String TERM_BUY = "购买";

  // 商品信息介绍相关关键词
  private static final String TERM_INTRO = "介绍";
  private static final String TERM_DETAIL = "详情";
  private static final String TERM_SELLING_POINT = "卖点";
  private static final String TERM_SPEC = "规格";
  private static final String TERM_MODEL = "型号";
  private static final String TERM_PARAM = "参数";
  private static final String TERM_SCENE = "场景";
  private static final String TERM_SUIT = "适合";

  // 助手身份与问候相关关键词
  private static final String TERM_WHO_ARE_YOU = "你是谁";
  private static final String TERM_WHAT_IS_YOUR_NAME = "你叫什么";
  private static final String TERM_WHAT_CAN_YOU_DO = "你能做什么";
  private static final String TERM_INTRODUCE_YOURSELF = "介绍一下你自己";
  private static final String TERM_HELLO = "你好";
  private static final String TERM_HELLO_POLITE = "您好";
  private static final String TERM_ARE_YOU_THERE = "在吗";
  private static final String TERM_WHY = "为什么";

  // 闲聊相关关键词
  private static final String TERM_CHAT = "闲聊";
  private static final String TERM_CHAT2 = "聊天";
  private static final String TERM_TALK = "聊聊";
  private static final String TERM_ACCOMPANY = "陪我";

  // 商品发现相关关键词
  private static final String TERM_DISCOVERY_ANY = "有没有";
  private static final String TERM_DISCOVERY_LIST = "有哪些";
  private static final String TERM_DISCOVERY_WHAT = "有什么";
  private static final String TERM_DISCOVERY_SELL = "卖什么";
  private static final String TERM_DISCOVERY_FIND = "找";
  private static final String TERM_DISCOVERY_SEARCH = "搜索";
  private static final String TERM_DISCOVERY_RECOMMEND = "推荐";
  private static final String TERM_DISCOVERY_BUY = "想买";
  private static final String TERM_DISCOVERY_MALL = "商城";

  // 依赖注入的服务与客户端
  private final ChatSessionService chatSessionService;           // 会话管理服务
  private final ChatRecordService chatRecordService;             // 对话记录服务
  private final ChatAuditService chatAuditService;               // 审计服务
  private final ChatJsonCodec chatJsonCodec;                     // JSON 编解码工具
  private final InMemoryKnowledgeStore knowledgeStore;           // 内存知识存储
  private final AiProperties aiProperties;                       // AI 配置属性
  private final List<ChatRouteStrategy> routeStrategies;         // 所有路由策略实现
  private final ProductKnowledgeClient productKnowledgeClient;   // 普通商品客户端
  private final SeckillKnowledgeClient seckillKnowledgeClient;   // 秒杀商品客户端

  /**
   * 处理用户对话请求
   *
   * 核心流程：
   * 1. 获取或创建会话
   * 2. 意图分类与问题改写
   * 3. 调用对应路由策略生成回答
   * 4. 持久化对话记录并更新会话状态
   *
   * @param userId  用户ID
   * @param request 对话请求DTO
   * @return 对话响应VO
   */
  @Override
  @Transactional
  public ChatResponseVO chat(Long userId, ChatRequestDTO request) {
    long startNanos = System.nanoTime();

    // 获取或创建会话
    ChatSessionPO session = chatSessionService.getOrCreate(
      request.getSessionId(),
      userId,
      request.getProductId(),
      request.getContextType()
    );
    ConversationContextState contextState = chatSessionService.getContextState(session);

    // 从请求中补充上下文信息（商品ID、名称、类型）
    hydrateContextFromRequest(request, session, contextState);
    Long effectiveProductId = resolveEffectiveProductId(request, session, contextState);

    // 意图分类与问题改写
    QuestionIntentType intentType = classifyIntent(request.getQuestion(), request.getContextType(), effectiveProductId, contextState);
    String rewrittenQuestion = rewriteQuestion(request.getQuestion(), intentType, contextState);
    QuestionCategory category = classifyCategory(rewrittenQuestion, request.getContextType(), intentType);
    OutOfScopeTopicType outOfScopeTopicType = intentType == QuestionIntentType.OUT_OF_SCOPE
      ? classifyOutOfScopeTopic(request.getQuestion())
      : null;

    log.info("AI route classified, sessionId={}, intentType={}, category={}, productId={}, rewrittenQuestion={}",
      session.getSessionId(), intentType, category, effectiveProductId, rewrittenQuestion);

    // 初始化上下文状态
    seedContextState(contextState, effectiveProductId, intentType, rewrittenQuestion);

    // 获取历史对话记录
    knowledgeStore.incrementChatRequests();
    List<ChatRecordPO> history = chatRecordService.listRecentHistory(session.getSessionId(), aiProperties.getHistoryLimit());

    // 构建路由请求并执行
    ChatRouteRequest routeRequest = buildRouteRequest(userId, request, session, effectiveProductId, intentType,
      outOfScopeTopicType, category, rewrittenQuestion, contextState, history);
    ChatRouteResult routeResult = resolveStrategy(intentType).execute(routeRequest);

    // 计算耗时与估算Token数
    long latencyMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    long estimatedTokens = estimateTokens(request.getQuestion(), routeResult.getAnswer());

    // 持久化结果
    persistChatResult(userId, request, session, effectiveProductId, routeResult, latencyMs, estimatedTokens);

    // 更新统计信息
    if (!routeResult.getHitKnowledge().isEmpty()) {
      knowledgeStore.incrementHitRequests();
    }
    if (StringUtils.hasText(routeResult.getFallbackReason())) {
      knowledgeStore.incrementFallbacks();
    }
    knowledgeStore.recordLatency(latencyMs, estimatedTokens);

    return toResponse(session.getSessionId(), routeResult);
  }

  /**
   * 构建路由请求对象
   */
  private ChatRouteRequest buildRouteRequest(Long userId, ChatRequestDTO request, ChatSessionPO session,
                                             Long effectiveProductId, QuestionIntentType intentType,
                                             OutOfScopeTopicType outOfScopeTopicType, QuestionCategory category,
                                             String rewrittenQuestion, ConversationContextState contextState,
                                             List<ChatRecordPO> history) {
    ChatRouteRequest routeRequest = new ChatRouteRequest();
    routeRequest.setUserId(userId);
    routeRequest.setOriginalRequest(request);
    routeRequest.setSession(session);
    routeRequest.setCurrentProductId(effectiveProductId);
    routeRequest.setCurrentProductName(contextState.getCurrentProductName());
    routeRequest.setCurrentProductType(contextState.getCurrentProductType());
    routeRequest.setQuestion(request.getQuestion());
    routeRequest.setRewrittenQuestion(rewrittenQuestion);
    routeRequest.setIntentType(intentType);
    routeRequest.setOutOfScopeTopicType(outOfScopeTopicType);
    routeRequest.setCategory(category);
    routeRequest.setContextState(contextState);
    routeRequest.setHistory(history);
    return routeRequest;
  }

  @Override
  public ChatSessionVO getSession(Long userId, String sessionId) {
    ChatSessionPO session = chatSessionService.getRequired(sessionId, userId);
    return chatRecordService.getSessionDetail(session, aiProperties.getSessionQueryLimit());
  }

  @Override
  public List<ChatSessionSummaryVO> listSessions(Long userId, Integer limit) {
    int actualLimit = limit == null ? 20 : limit;
    return chatSessionService.listByUserId(userId, actualLimit);
  }

  @Override
  @Transactional
  public void deleteSession(Long userId, String sessionId) {
    chatSessionService.getRequired(sessionId, userId);
    chatRecordService.deleteBySessionId(sessionId);
    chatSessionService.deleteSession(sessionId, userId);
  }

  /**
   * 根据意图类型解析对应的路由策略
   */
  private ChatRouteStrategy resolveStrategy(QuestionIntentType intentType) {
    Map<QuestionIntentType, ChatRouteStrategy> strategyMap = new EnumMap<>(QuestionIntentType.class);
    for (ChatRouteStrategy strategy : routeStrategies) {
      strategyMap.put(strategy.supports(), strategy);
    }
    return strategyMap.getOrDefault(intentType, strategyMap.get(QuestionIntentType.OUT_OF_SCOPE));
  }

  /**
   * 持久化对话结果
   *
   * 包括保存对话记录和更新会话状态（最后提问时间、上下文状态等）
   */
  private void persistChatResult(Long userId, ChatRequestDTO request, ChatSessionPO session, Long productId,
                                 ChatRouteResult result, long latencyMs, long estimatedTokens) {
    String auditSummary = chatAuditService.buildAuditSummary(
      request.getQuestion(),
      result.getAnswer(),
      result.getAnswerPolicy(),
      result.getFallbackReason(),
      result.getHitKnowledge()
    );

    // 构建并保存对话记录
    ChatRecordPO record = new ChatRecordPO();
    record.setSessionId(session.getSessionId());
    record.setUserId(userId);
    record.setProductId(productId);
    record.setQuestion(request.getQuestion());
    record.setQuestionCategory(result.getCategory().name());
    record.setIntentType(result.getIntentType().name());
    record.setRouteType(result.getRouteType());
    record.setRewrittenQuestion(result.getRewrittenQuestion());
    record.setAnswer(result.getAnswer());
    record.setAnswerPolicy(result.getAnswerPolicy().name());
    record.setSourcesJson(chatJsonCodec.writeStringList(result.getSources()));
    record.setHitKnowledgeJson(chatJsonCodec.writeKnowledgeList(result.getHitKnowledge()));
    record.setCompareCandidatesJson(chatJsonCodec.writeCandidateList(result.getCompareCandidates()));
    record.setConfidence(BigDecimal.valueOf(result.getConfidence()));
    record.setFallbackReason(result.getFallbackReason());
    record.setAuditSummary(auditSummary);
    record.setModelName(aiProperties.getChatModel());
    record.setLatencyMs((int) latencyMs);
    record.setEstimatedTokens((int) estimatedTokens);
    record.setCreatedAt(LocalDateTime.now());
    record.setExpireAt(record.getCreatedAt().plusDays(aiProperties.getSessionTtlDays()));
    chatRecordService.save(record);

    // 更新会话上下文状态
    ConversationContextState contextState = result.getContextState();
    hydrateContextIdentity(contextState, productId, request.getProductName(), request.getProductType());
    contextState.setCurrentIntentType(result.getIntentType().name());
    contextState.setLastQuestion(request.getQuestion());
    contextState.setLastAnswerSummary(summarizeAnswer(result.getAnswer()));
    contextState.setRewrittenQuestion(result.getRewrittenQuestion());
    if (productId != null) {
      contextState.setCurrentProductId(productId);
    }

    // 刷新会话信息
    chatSessionService.refreshSession(
      session,
      request.getQuestion(),
      summarizeAnswer(result.getAnswer()),
      productId,
      request.getContextType(),
      contextState
    );
  }

  /**
   * 将路由结果转换为响应VO
   */
  private ChatResponseVO toResponse(String sessionId, ChatRouteResult result) {
    ChatResponseVO response = new ChatResponseVO();
    response.setSessionId(sessionId);
    response.setCategory(result.getCategory().name());
    response.setIntentType(result.getIntentType().name());
    response.setRouteType(result.getRouteType());
    response.setRewrittenQuestion(result.getRewrittenQuestion());
    response.setContextState(result.getContextState());
    response.setAnswer(result.getAnswer());
    response.setSources(result.getSources());
    response.setHitKnowledge(result.getHitKnowledge());
    response.setCompareCandidates(result.getCompareCandidates());
    response.setConfidence(result.getConfidence());
    response.setFallbackReason(result.getFallbackReason());
    response.setAnswerPolicy(result.getAnswerPolicy().name());
    return response;
  }

  /**
   * 解析当前生效的商品ID
   *
   * 优先级：请求参数 > 上下文状态 > 会话锚定商品
   */
  private Long resolveEffectiveProductId(ChatRequestDTO request, ChatSessionPO session, ConversationContextState contextState) {
    if (request.getProductId() != null) {
      return request.getProductId();
    }
    if (contextState.getCurrentProductId() != null) {
      return contextState.getCurrentProductId();
    }
    return session.getProductId();
  }

  /**
   * 从请求中补充上下文信息
   *
   * 包括商品ID、商品名称、商品类型，并初始化对比候选列表
   */
  private void hydrateContextFromRequest(ChatRequestDTO request, ChatSessionPO session, ConversationContextState contextState) {
    // 初始化集合字段，避免 NPE
    if (contextState.getCompareCandidateIds() == null) {
      contextState.setCompareCandidateIds(List.of());
    }
    if (contextState.getCompareCandidateNames() == null) {
      contextState.setCompareCandidateNames(List.of());
    }

    Long requestProductId = request.getProductId();
    if (requestProductId != null) {
      // 请求中明确携带商品ID时，更新上下文
      if (!requestProductId.equals(contextState.getCurrentProductId())) {
        contextState.setCurrentProductId(requestProductId);
      }
      if (StringUtils.hasText(request.getProductName())) {
        contextState.setCurrentProductName(request.getProductName().trim());
      }
      if (StringUtils.hasText(request.getProductType())) {
        contextState.setCurrentProductType(request.getProductType().trim());
      }
    } else if (contextState.getCurrentProductId() == null && session.getProductId() != null) {
      // 旧会话可能没有请求参数，沿用会话中的商品锚点
      contextState.setCurrentProductId(session.getProductId());
    }

    // 补充商品名称和类型（如果缺失）
    hydrateContextIdentity(
      contextState,
      resolveEffectiveProductId(request, session, contextState),
      request.getProductName(),
      request.getProductType()
    );
  }

  /**
   * 补全上下文中的商品标识信息（名称、类型）
   *
   * 当上下文中缺失商品名称或类型时，尝试从知识库客户端获取
   */
  private void hydrateContextIdentity(ConversationContextState contextState, Long productId,
                                      String requestProductName, String requestProductType) {
    if (productId == null) {
      return;
    }

    // 优先使用请求中携带的信息
    if (StringUtils.hasText(requestProductName)) {
      contextState.setCurrentProductName(requestProductName.trim());
    }
    if (StringUtils.hasText(requestProductType)) {
      contextState.setCurrentProductType(requestProductType.trim());
    }
    if (StringUtils.hasText(contextState.getCurrentProductName()) && StringUtils.hasText(contextState.getCurrentProductType())) {
      return;
    }

    // 缺失时从数据源获取
    ProductIdentity identity = fetchProductIdentity(productId, contextState.getCurrentProductType(), contextState.getCurrentProductName());
    if (identity == null) {
      return;
    }
    if (!StringUtils.hasText(contextState.getCurrentProductName())) {
      contextState.setCurrentProductName(identity.productName());
    }
    if (!StringUtils.hasText(contextState.getCurrentProductType())) {
      contextState.setCurrentProductType(identity.productType());
    }
  }

  /**
   * 获取商品身份信息（名称和类型）
   *
   * 优先按指定类型查找，若类型不明确则依次尝试普通商品和秒杀商品
   */
  private ProductIdentity fetchProductIdentity(Long productId, String preferredType, String preferredName) {
    if (productId == null) {
      return null;
    }

    ProductIdentity normalIdentity = null;
    ProductIdentity seckillIdentity = null;

    // 优先按指定类型查找
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

    // 如果调用方提供了商品名称，利用名称辅助区分同一ID可能对应的两种商品类型
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

    // 默认返回普通商品，不存在则返回秒杀商品
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

  private ProductIdentity fetchNormalIdentity(Long productId) {
    ProductKnowledgeDTO product = productKnowledgeClient.getProductById(productId);
    if (product == null || !StringUtils.hasText(product.getName())) {
      return null;
    }
    return new ProductIdentity(product.getName(), PRODUCT_TYPE_NORMAL);
  }

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

  private String normalizeProductName(String productName) {
    if (!StringUtils.hasText(productName)) {
      return "";
    }
    return productName.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
  }

  /**
   * 初始化上下文状态（设置对比候选列表、当前意图等）
   */
  private void seedContextState(ConversationContextState contextState, Long productId,
                                QuestionIntentType intentType, String rewrittenQuestion) {
    if (contextState.getCompareCandidateIds() == null) {
      contextState.setCompareCandidateIds(List.of());
    }
    if (contextState.getCompareCandidateNames() == null) {
      contextState.setCompareCandidateNames(List.of());
    }
    if (productId != null) {
      contextState.setCurrentProductId(productId);
    }
    // 非对比意图时清空对比候选列表
    if (intentType != QuestionIntentType.COMPARE_RECOMMENDATION) {
      contextState.setCompareCandidateIds(List.of());
      contextState.setCompareCandidateNames(List.of());
    }
    contextState.setCurrentIntentType(intentType.name());
    contextState.setRewrittenQuestion(rewrittenQuestion);
  }

  /**
   * 意图分类
   *
   * 基于关键词规则识别用户意图：
   * - 问候/身份询问
   * - 越界话题
   * - 对比推荐
   * - 政策问答
   * - 实时状态
   * - 商品发现
   * - 商品事实
   *
   * @param question       原始问题
   * @param contextType    上下文类型
   * @param productId      当前商品ID
   * @param contextState   上下文状态
   * @return 意图类型
   */
  private QuestionIntentType classifyIntent(String question, String contextType, Long productId, ConversationContextState contextState) {
    String normalizedQuestion = normalize(question);
    boolean hasProductContext = hasProductContext(contextType, productId, contextState);

    // 问候或身份询问
    if (isAssistantIdentityQuestion(normalizedQuestion) || isGreetingOnly(normalizedQuestion)) {
      return QuestionIntentType.GREETING_IDENTITY;
    }
    // 越界话题检测
    if (classifyOutOfScopeTopic(question) != OutOfScopeTopicType.UNKNOWN) {
      return QuestionIntentType.OUT_OF_SCOPE;
    }
    // 对比推荐（需有商品上下文）
    if (hasProductContext && isExplicitCompareIntent(normalizedQuestion)) {
      return QuestionIntentType.COMPARE_RECOMMENDATION;
    }
    // 政策问答
    if (containsAny(normalizedQuestion, TERM_REFUND, TERM_RETURN, TERM_AFTER_SALES, TERM_WARRANTY,
      TERM_DELIVERY, TERM_SHIPPING, TERM_EXPRESS, TERM_LOGISTICS, TERM_FREIGHT)) {
      return QuestionIntentType.POLICY_QA;
    }
    // 实时状态
    if (containsAny(normalizedQuestion, TERM_INVENTORY, TERM_AVAILABLE, TERM_PRICE, TERM_HOW_MUCH, TERM_SECKILL_PRICE,
      TERM_SECKILL, TERM_ACTIVITY, TERM_START, TERM_END)) {
      return QuestionIntentType.REALTIME_STATUS;
    }
    // 商品发现
    if (isDiscoveryQuestion(normalizedQuestion, hasProductContext)) {
      return QuestionIntentType.PRODUCT_DISCOVERY;
    }
    // 无上下文的对比推荐相关词汇视为越界
    if (isExplicitCompareIntent(normalizedQuestion)) {
      return QuestionIntentType.OUT_OF_SCOPE;
    }
    // 商品事实问答
    if (hasBusinessSignals(normalizedQuestion) || hasProductContext || mentionsKnownProduct(normalizedQuestion)) {
      return QuestionIntentType.PRODUCT_FACT;
    }

    return QuestionIntentType.OUT_OF_SCOPE;
  }

  /**
   * 细分越界话题类型
   */
  private OutOfScopeTopicType classifyOutOfScopeTopic(String question) {
    String normalizedQuestion = normalize(question);
    if (containsAny(normalizedQuestion, QUESTION_WEATHER)) {
      return OutOfScopeTopicType.WEATHER;
    }
    if (containsAny(normalizedQuestion, QUESTION_STOCK, QUESTION_FUND, QUESTION_FINANCIAL, QUESTION_MARKET)) {
      return OutOfScopeTopicType.FINANCE;
    }
    if (containsAny(normalizedQuestion, QUESTION_CODE, QUESTION_PROGRAM, QUESTION_BUG)) {
      return OutOfScopeTopicType.TECH;
    }
    if (containsAny(normalizedQuestion, QUESTION_DOCTOR, QUESTION_HOSPITAL, QUESTION_MEDICINE)) {
      return OutOfScopeTopicType.MEDICAL;
    }
    if (containsAny(normalizedQuestion, QUESTION_LAW, QUESTION_LAWYER)) {
      return OutOfScopeTopicType.LEGAL;
    }
    if (containsAny(normalizedQuestion, QUESTION_PRESIDENT, QUESTION_POLICY, QUESTION_NEWS)) {
      return OutOfScopeTopicType.POLITICS;
    }
    if (containsAny(normalizedQuestion, TERM_CHAT, TERM_CHAT2, TERM_TALK, TERM_ACCOMPANY)) {
      return OutOfScopeTopicType.GENERAL_CHAT;
    }
    return OutOfScopeTopicType.UNKNOWN;
  }

  /**
   * 问题类别细分
   */
  private QuestionCategory classifyCategory(String question, String contextType, QuestionIntentType intentType) {
    if (intentType == QuestionIntentType.OUT_OF_SCOPE || intentType == QuestionIntentType.GREETING_IDENTITY) {
      return QuestionCategory.OUT_OF_SCOPE;
    }
    if (intentType == QuestionIntentType.PRODUCT_DISCOVERY) {
      return QuestionCategory.PRODUCT_DISCOVERY;
    }
    String normalizedQuestion = normalize(question);
    if (intentType == QuestionIntentType.COMPARE_RECOMMENDATION) {
      return QuestionCategory.COMPARE_RECOMMENDATION;
    }
    if (intentType == QuestionIntentType.POLICY_QA) {
      if (containsAny(normalizedQuestion, TERM_DELIVERY, TERM_SHIPPING, TERM_EXPRESS, TERM_LOGISTICS, TERM_FREIGHT)) {
        return QuestionCategory.DELIVERY_POLICY;
      }
      return QuestionCategory.AFTER_SALES_POLICY;
    }
    if (intentType == QuestionIntentType.REALTIME_STATUS) {
      if (containsAny(normalizedQuestion, TERM_SECKILL, TERM_ACTIVITY, TERM_START, TERM_END)) {
        return QuestionCategory.ACTIVITY_RULE;
      }
      return QuestionCategory.REALTIME_STATUS;
    }
    if (contextType != null && contextType.toLowerCase(Locale.ROOT).contains("product")) {
      return QuestionCategory.PRODUCT_INFO;
    }
    return QuestionCategory.PRODUCT_INFO;
  }

  /**
   * 问题改写
   *
   * 将问题中的代词（"这款"、"它"等）替换为当前商品名称，补充上下文信息
   */
  private String rewriteQuestion(String question, QuestionIntentType intentType, ConversationContextState contextState) {
    if (!StringUtils.hasText(question)) {
      return "";
    }
    String rewritten = question.trim();
    String currentProductName = contextState.getCurrentProductName();

    // 代词替换
    if (StringUtils.hasText(currentProductName)) {
      rewritten = rewritten
        .replace(TERM_THIS_PRODUCT, currentProductName)
        .replace(TERM_THIS_ITEM, currentProductName)
        .replace(TERM_THIS, currentProductName)
        .replace(TERM_IT, currentProductName)
        .replace(TERM_THAT, currentProductName);
      if (intentType == QuestionIntentType.COMPARE_RECOMMENDATION && !rewritten.contains(currentProductName)) {
        rewritten = "围绕" + currentProductName + "，" + rewritten;
      }
    }

    // 处理追问"为什么"
    if (rewritten.equals(TERM_WHY) && StringUtils.hasText(contextState.getLastQuestion())) {
      rewritten = "针对上一个问题「" + contextState.getLastQuestion() + "」，请解释原因。";
    }

    // 商品事实类问题补充商品名
    if (intentType == QuestionIntentType.PRODUCT_FACT && StringUtils.hasText(currentProductName) && rewritten.length() <= 12) {
      rewritten = "关于" + currentProductName + "，" + rewritten;
    }
    return rewritten;
  }

  /**
   * 判断是否为商品发现问题
   */
  private boolean isDiscoveryQuestion(String normalizedQuestion, boolean hasProductContext) {
    if (!StringUtils.hasText(normalizedQuestion)) {
      return false;
    }

    // 明确的发现关键词
    boolean explicitDiscovery = containsAny(
      normalizedQuestion,
      TERM_DISCOVERY_ANY,
      TERM_DISCOVERY_LIST,
      TERM_DISCOVERY_WHAT,
      TERM_DISCOVERY_SELL,
      TERM_DISCOVERY_FIND,
      TERM_DISCOVERY_SEARCH
    ) || (normalizedQuestion.contains(TERM_DISCOVERY_MALL) && normalizedQuestion.contains(TERM_PRODUCT));

    if (explicitDiscovery) {
      // 若有明确商品上下文且问题包含指代词，则不是纯发现意图
      return !(hasProductContext && hasCurrentProductReference(normalizedQuestion));
    }
    // 无商品上下文时的推荐/购买意图视为商品发现
    return !hasProductContext && containsAny(normalizedQuestion, TERM_DISCOVERY_RECOMMEND, TERM_DISCOVERY_BUY);
  }

  /**
   * 检查问题中是否包含当前商品的指代词
   */
  private boolean hasCurrentProductReference(String normalizedQuestion) {
    return containsAny(normalizedQuestion, TERM_THIS_PRODUCT, TERM_THIS_ITEM, TERM_THIS, TERM_IT, TERM_THAT);
  }

  /**
   * 检查问题中是否包含业务相关信号（商品介绍、参数等）
   */
  private boolean hasBusinessSignals(String normalizedQuestion) {
    return containsAny(
      normalizedQuestion,
      TERM_PRODUCT,
      TERM_THIS_PRODUCT,
      TERM_THIS_ITEM,
      TERM_THIS,
      TERM_IT,
      TERM_THAT,
      TERM_INTRO,
      TERM_DETAIL,
      TERM_SELLING_POINT,
      TERM_SPEC,
      TERM_MODEL,
      TERM_PARAM,
      TERM_SCENE,
      TERM_SUIT
    );
  }

  /**
   * compare intent 鍙鍙槑纭殑瀵规瘮淇″彿銆?
   * 鈥滄帹鑽?鎯充拱/鍊煎緱涔扳€濇洿鍍忚喘涔板缓璁垨鍟嗗搧闂瓟锛屼笉搴旂洿鎺ヨ鍒ゆ柇涓?compare銆?
   */
  private boolean isExplicitCompareIntent(String normalizedQuestion) {
    return containsAny(
      normalizedQuestion,
      TERM_COMPARE,
      TERM_COMPARE2,
      TERM_COMPARE3,
      TERM_ADVANTAGE,
      TERM_DISADVANTAGE
    ) || normalizedQuestion.contains("鍝釜鏇村ソ")
      || normalizedQuestion.contains("鏇夸唬")
      || normalizedQuestion.contains("鍝釜鏇村€煎緱");
  }

  /**
   * 判断是否存在商品上下文（请求参数、会话锚定或上下文中已有商品信息）
   */
  private boolean hasProductContext(String contextType, Long productId, ConversationContextState contextState) {
    return productId != null
      || contextState.getCurrentProductId() != null
      || StringUtils.hasText(contextState.getCurrentProductName())
      || (contextType != null && contextType.toLowerCase(Locale.ROOT).contains("product"));
  }

  /**
   * 判断是否为助手身份询问
   */
  private boolean isAssistantIdentityQuestion(String normalizedQuestion) {
    return containsAny(normalizedQuestion, TERM_WHO_ARE_YOU, TERM_WHAT_IS_YOUR_NAME, TERM_WHAT_CAN_YOU_DO, TERM_INTRODUCE_YOURSELF);
  }

  /**
   * 判断是否为纯问候语
   */
  private boolean isGreetingOnly(String normalizedQuestion) {
    String compactQuestion = normalizedQuestion
      .replace("，", "")
      .replace("。", "")
      .replace("！", "")
      .replace("？", "")
      .replace(",", "")
      .replace(".", "")
      .replace("!", "")
      .replace("?", "")
      .trim();
    return compactQuestion.equals(TERM_HELLO)
      || compactQuestion.equals(TERM_HELLO_POLITE)
      || compactQuestion.equals("hi")
      || compactQuestion.equals("hello")
      || compactQuestion.equals(TERM_ARE_YOU_THERE);
  }

  /**
   * 检查问题中是否提及了知识库中已知的商品名称
   */
  private boolean mentionsKnownProduct(String normalizedQuestion) {
    if (!StringUtils.hasText(normalizedQuestion)) {
      return false;
    }
    return knowledgeStore.getAllChunks().stream()
      .filter(chunk -> chunk.getSourceType() != null)
      .filter(chunk -> "PRODUCT".equalsIgnoreCase(chunk.getSourceType().name())
        || "SECKILL".equalsIgnoreCase(chunk.getSourceType().name()))
      .map(chunk -> compact(chunk.getTitle()))
      .filter(StringUtils::hasText)
      .anyMatch(compactTitle -> compact(normalizedQuestion).contains(compactTitle));
  }

  /**
   * 文本规范化（转小写、去首尾空格）
   */
  private String normalize(String question) {
    return question == null ? "" : question.toLowerCase(Locale.ROOT).trim();
  }

  /**
   * 文本紧凑化（去除非字母数字和中文的字符）
   */
  private String compact(String text) {
    if (!StringUtils.hasText(text)) {
      return "";
    }
    return text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\u4e00-\\u9fa5]+", "");
  }

  /**
   * 检查文本是否包含任意关键词
   */
  private boolean containsAny(String text, String... keywords) {
    for (String keyword : keywords) {
      if (text.contains(keyword)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 生成回答摘要（截取前120字符）
   */
  private String summarizeAnswer(String answer) {
    if (answer == null) {
      return "";
    }
    return answer.length() <= 120 ? answer : answer.substring(0, 120);
  }

  /**
   * 估算Token消耗量（粗略按每4字符1 Token计算）
   */
  private long estimateTokens(String question, String answer) {
    int length = (question == null ? 0 : question.length()) + (answer == null ? 0 : answer.length());
    return Math.max(1L, Math.round(length / 4.0));
  }

  /**
   * 商品身份信息记录（内部使用）
   */
  private record ProductIdentity(String productName, String productType) {
  }
}
