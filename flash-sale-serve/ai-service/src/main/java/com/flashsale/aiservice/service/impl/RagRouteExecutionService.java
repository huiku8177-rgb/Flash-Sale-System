package com.flashsale.aiservice.service.impl;

import com.flashsale.aiservice.client.ChatModelClient;
import com.flashsale.aiservice.client.EmbeddingClient;
import com.flashsale.aiservice.client.ProductKnowledgeClient;
import com.flashsale.aiservice.client.SeckillKnowledgeClient;
import com.flashsale.aiservice.config.AiProperties;
import com.flashsale.aiservice.domain.dto.ProductKnowledgeDTO;
import com.flashsale.aiservice.domain.dto.SeckillKnowledgeDTO;
import com.flashsale.aiservice.domain.enums.AnswerPolicy;
import com.flashsale.aiservice.domain.enums.OutOfScopeTopicType;
import com.flashsale.aiservice.domain.enums.QuestionCategory;
import com.flashsale.aiservice.domain.enums.QuestionIntentType;
import com.flashsale.aiservice.domain.vo.ProductCandidateVO;
import com.flashsale.aiservice.domain.vo.RelatedKnowledgeVO;
import com.flashsale.aiservice.exception.ModelInvokeException;
import com.flashsale.aiservice.service.KnowledgeRetrievalService;
import com.flashsale.aiservice.service.PromptBuilderService;
import com.flashsale.aiservice.service.model.KnowledgeRetrieveRequest;
import com.flashsale.aiservice.service.model.PromptBuildRequest;
import com.flashsale.aiservice.service.route.ChatRouteRequest;
import com.flashsale.aiservice.service.route.ChatRouteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * RAG 路由执行服务
 *
 * 负责根据意图分类执行不同的回答策略，包括：
 * - 欢迎语和越界拒答
 * - 商品发现（关键词/模糊搜索）
 * - 商品事实问答（基于向量检索+大模型生成）
 * - 实时状态查询（价格、库存、秒杀进度）
 * - 政策规则问答
 * - 商品对比推荐
 *
 * 该服务集成了向量检索、知识库客户端、大模型调用以及降级容错逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagRouteExecutionService {

  // 商品类型常量
  private static final String PRODUCT_TYPE_NORMAL = "normal";
  private static final String PRODUCT_TYPE_SECKILL = "seckill";

  // 路由类型标识常量
  public static final String ROUTE_GREETING = "GREETING_ANSWER";
  public static final String ROUTE_OUT_OF_SCOPE = "OUT_OF_SCOPE_REFUSAL";
  public static final String ROUTE_OUT_OF_SCOPE_WEATHER = "OUT_OF_SCOPE_WEATHER";
  public static final String ROUTE_OUT_OF_SCOPE_FINANCE = "OUT_OF_SCOPE_FINANCE";
  public static final String ROUTE_OUT_OF_SCOPE_TECH = "OUT_OF_SCOPE_TECH";
  public static final String ROUTE_OUT_OF_SCOPE_MEDICAL = "OUT_OF_SCOPE_MEDICAL";
  public static final String ROUTE_OUT_OF_SCOPE_LEGAL = "OUT_OF_SCOPE_LEGAL";
  public static final String ROUTE_OUT_OF_SCOPE_POLITICS = "OUT_OF_SCOPE_POLITICS";
  public static final String ROUTE_OUT_OF_SCOPE_CHAT = "OUT_OF_SCOPE_GENERAL_CHAT";
  public static final String ROUTE_DISCOVERY = "PRODUCT_DISCOVERY_SEARCH";
  public static final String ROUTE_PRODUCT_FACT = "PRODUCT_FACT_RAG";
  public static final String ROUTE_REALTIME = "REALTIME_STATUS_RAG";
  public static final String ROUTE_POLICY = "POLICY_QA_RAG";
  public static final String ROUTE_COMPARE = "COMPARE_RECOMMENDATION_RAG";

  // 降级原因常量
  private static final String FALLBACK_NO_RELEVANT_KNOWLEDGE = "NO_RELEVANT_KNOWLEDGE";
  private static final String FALLBACK_KNOWLEDGE_NOT_READY = "KNOWLEDGE_NOT_READY";
  private static final String FALLBACK_EMBEDDING_UNAVAILABLE = "EMBEDDING_UNAVAILABLE";
  private static final String FALLBACK_RETRIEVAL_UNAVAILABLE = "RETRIEVAL_UNAVAILABLE";
  private static final String FALLBACK_MODEL_UNAVAILABLE = "MODEL_UNAVAILABLE";
  private static final String FALLBACK_OUT_OF_SCOPE = "OUT_OF_SCOPE";
  private static final String FALLBACK_GREETING = "GREETING";
  private static final String FALLBACK_NO_DISCOVERY_RESULTS = "NO_DISCOVERY_RESULTS";

  // 依赖注入
  private final EmbeddingClient embeddingClient;                 // 向量化客户端
  private final ChatModelClient chatModelClient;                 // 大模型调用客户端
  private final ProductKnowledgeClient productKnowledgeClient;   // 普通商品知识客户端
  private final SeckillKnowledgeClient seckillKnowledgeClient;   // 秒杀商品知识客户端
  private final KnowledgeRetrievalService knowledgeRetrievalService; // 知识检索服务
  private final PromptBuilderService promptBuilderService;       // Prompt 构建服务
  private final InMemoryKnowledgeStore knowledgeStore;           // 内存知识存储（用于降级检索）
  private final AiProperties aiProperties;                       // AI 相关配置属性

  /**
   * 处理欢迎语/身份介绍类问题
   *
   * @param request 请求上下文
   * @return 固定欢迎语结果
   */
  public ChatRouteResult greeting(ChatRouteRequest request) {
    return fixedResult(
      "你好，我可以帮你解答商品信息、实时价格与库存、秒杀活动、配送和售后等问题。",
      QuestionCategory.OUT_OF_SCOPE,
      QuestionIntentType.GREETING_IDENTITY,
      ROUTE_GREETING,
      FALLBACK_GREETING,
      AnswerPolicy.FIXED_TEMPLATE,
      request
    );
  }

  /**
   * 处理超出服务范围的问题（通用拒答）
   *
   * @param request 请求上下文
   * @return 固定拒答话术结果
   */
  public ChatRouteResult outOfScope(ChatRouteRequest request) {
    return fixedResult(
      "我目前主要回答商品信息、活动规则、价格库存、配送和售后问题。请换一个和商品相关的问题。",
      QuestionCategory.OUT_OF_SCOPE,
      QuestionIntentType.OUT_OF_SCOPE,
      ROUTE_OUT_OF_SCOPE,
      FALLBACK_OUT_OF_SCOPE,
      AnswerPolicy.OUT_OF_SCOPE_REFUSAL,
      request
    );
  }

  /**
   * 商品事实问答（基于RAG检索增强）
   *
   * @param request 请求上下文
   * @return 路由执行结果
   */
  public ChatRouteResult productFact(ChatRouteRequest request) {
    return executeKnowledgeRoute(request, ROUTE_PRODUCT_FACT, false, List.of());
  }

  /**
   * 商品发现（模糊搜索/品类浏览）
   *
   * @param request 请求上下文
   * @return 候选商品列表及回答
   */
  public ChatRouteResult productDiscovery(ChatRouteRequest request) {
    List<ProductCandidateVO> candidates = discoverProducts(request);
    // 无精确匹配时尝试宽泛推荐
    if (candidates.isEmpty()) {
      List<ProductCandidateVO> broadCandidates = mergeAndRankBroadDiscovery(6);
      if (!broadCandidates.isEmpty()) {
        log.info("Product discovery fallback to broad catalog, sessionId={}, question={}, candidateCount={}",
          request.getSession().getSessionId(), request.getQuestion(), broadCandidates.size());
        return buildDiscoveryResult(
          buildDiscoveryFallbackAnswer(request.getQuestion(), broadCandidates),
          broadCandidates,
          0.25d,
          FALLBACK_NO_DISCOVERY_RESULTS,
          request
        );
      }
      return buildDiscoveryResult(
        "暂时没有找到直接匹配的商品。你可以补充更具体的商品名、品牌或品类。",
        List.of(),
        0d,
        FALLBACK_NO_DISCOVERY_RESULTS,
        request
      );
    }

    log.info("Product discovery matched candidates, sessionId={}, question={}, candidateCount={}, topCandidate={}",
      request.getSession().getSessionId(), request.getQuestion(), candidates.size(), candidates.get(0).getName());
    String answer = buildDiscoveryAnswer(request.getQuestion(), candidates, isBroadDiscoveryQuestion(request.getQuestion()));
    return buildDiscoveryResult(answer, candidates,
      Math.min(0.98d, candidates.get(0).getScore()),
      null,
      request);
  }

  /**
   * 实时状态查询（价格、库存、秒杀进度）
   *
   * @param request 请求上下文
   * @return 路由执行结果
   */
  public ChatRouteResult realtimeStatus(ChatRouteRequest request) {
    return executeKnowledgeRoute(request, ROUTE_REALTIME, true, List.of());
  }

  /**
   * 政策/规则问答（售后、配送、活动规则等）
   *
   * @param request 请求上下文
   * @return 路由执行结果
   */
  public ChatRouteResult policyQa(ChatRouteRequest request) {
    return executeKnowledgeRoute(request, ROUTE_POLICY, false, List.of());
  }

  /**
   * 商品对比推荐
   *
   * @param request 请求上下文
   * @return 路由执行结果
   */
  public ChatRouteResult compareRecommendation(ChatRouteRequest request) {
    List<ProductCandidateVO> candidates = discoverCompareCandidates(request);
    request.getContextState().setCompareCandidateIds(candidates.stream().map(ProductCandidateVO::getProductId).toList());
    request.getContextState().setCompareCandidateNames(candidates.stream().map(ProductCandidateVO::getName).toList());
    return executeKnowledgeRoute(request, ROUTE_COMPARE, false, candidates);
  }

  /**
   * 细分领域的越界拒答（天气、金融、技术等）
   *
   * @param request 请求上下文（需包含越界主题类型）
   * @return 对应的拒答话术结果
   */
  public ChatRouteResult dynamicOutOfScope(ChatRouteRequest request) {
    OutOfScopeTopicType topicType = request.getOutOfScopeTopicType() == null
      ? OutOfScopeTopicType.UNKNOWN
      : request.getOutOfScopeTopicType();
    return switch (topicType) {
      case WEATHER -> fixedResult(
        "我目前不提供天气查询，但可以继续帮你解答商品信息、价格库存、活动时间、配送和售后问题。",
        QuestionCategory.OUT_OF_SCOPE,
        QuestionIntentType.OUT_OF_SCOPE,
        ROUTE_OUT_OF_SCOPE_WEATHER,
        FALLBACK_OUT_OF_SCOPE,
        AnswerPolicy.OUT_OF_SCOPE_REFUSAL,
        request
      );
      case FINANCE -> fixedResult(
        "我不提供股票、基金或投资建议。目前主要聚焦商城商品、活动、价格库存、配送和售后问题。",
        QuestionCategory.OUT_OF_SCOPE,
        QuestionIntentType.OUT_OF_SCOPE,
        ROUTE_OUT_OF_SCOPE_FINANCE,
        FALLBACK_OUT_OF_SCOPE,
        AnswerPolicy.OUT_OF_SCOPE_REFUSAL,
        request
      );
      case TECH -> fixedResult(
        "我不提供编程或技术支持。如果你想了解商品、价格库存、活动、配送或售后，我可以继续帮你。",
        QuestionCategory.OUT_OF_SCOPE,
        QuestionIntentType.OUT_OF_SCOPE,
        ROUTE_OUT_OF_SCOPE_TECH,
        FALLBACK_OUT_OF_SCOPE,
        AnswerPolicy.OUT_OF_SCOPE_REFUSAL,
        request
      );
      case MEDICAL -> fixedResult(
        "我不提供医疗或用药建议。目前主要聚焦商城商品、活动、价格库存、配送和售后问题。",
        QuestionCategory.OUT_OF_SCOPE,
        QuestionIntentType.OUT_OF_SCOPE,
        ROUTE_OUT_OF_SCOPE_MEDICAL,
        FALLBACK_OUT_OF_SCOPE,
        AnswerPolicy.OUT_OF_SCOPE_REFUSAL,
        request
      );
      case LEGAL -> fixedResult(
        "我不提供法律或合规建议。目前主要聚焦商城商品、活动、价格库存、配送和售后问题。",
        QuestionCategory.OUT_OF_SCOPE,
        QuestionIntentType.OUT_OF_SCOPE,
        ROUTE_OUT_OF_SCOPE_LEGAL,
        FALLBACK_OUT_OF_SCOPE,
        AnswerPolicy.OUT_OF_SCOPE_REFUSAL,
        request
      );
      case POLITICS -> fixedResult(
        "我不提供政治或公共事务分析。目前主要聚焦商城商品和客服相关问题。",
        QuestionCategory.OUT_OF_SCOPE,
        QuestionIntentType.OUT_OF_SCOPE,
        ROUTE_OUT_OF_SCOPE_POLITICS,
        FALLBACK_OUT_OF_SCOPE,
        AnswerPolicy.OUT_OF_SCOPE_REFUSAL,
        request
      );
      case GENERAL_CHAT -> fixedResult(
        "我当前是商城商品助手，不是通用聊天机器人。如果你想了解商品、价格库存、活动、配送或售后，我可以继续帮你。",
        QuestionCategory.OUT_OF_SCOPE,
        QuestionIntentType.OUT_OF_SCOPE,
        ROUTE_OUT_OF_SCOPE_CHAT,
        FALLBACK_OUT_OF_SCOPE,
        AnswerPolicy.OUT_OF_SCOPE_REFUSAL,
        request
      );
      case UNKNOWN -> fixedResult(
        "这个问题超出了我当前的服务范围。我目前主要回答商品信息、活动规则、价格库存、配送和售后问题。",
        QuestionCategory.OUT_OF_SCOPE,
        QuestionIntentType.OUT_OF_SCOPE,
        ROUTE_OUT_OF_SCOPE,
        FALLBACK_OUT_OF_SCOPE,
        AnswerPolicy.OUT_OF_SCOPE_REFUSAL,
        request
      );
    };
  }

  /**
   * 执行知识路由的核心流程：
   * 1. 构建上下文（检索知识、实时信息）
   * 2. 判断是否可调用大模型
   * 3. 调用大模型或触发降级
   *
   * @param request             请求
   * @param routeType           路由类型
   * @param includeRealtimeFacts 是否包含实时信息
   * @param compareCandidates   对比候选商品（对比场景使用）
   * @return 路由结果
   */
  private ChatRouteResult executeKnowledgeRoute(ChatRouteRequest request, String routeType,
                                                boolean includeRealtimeFacts, List<ProductCandidateVO> compareCandidates) {
    RouteKnowledgeContext context = buildRouteKnowledgeContext(request, routeType, includeRealtimeFacts, compareCandidates);
    if (!canInvokeModel(context)) {
      return buildFallbackResult(context);
    }

    PromptBuildRequest promptRequest = new PromptBuildRequest();
    promptRequest.setQuestion(request.getQuestion());
    promptRequest.setRewrittenQuestion(request.getRewrittenQuestion());
    promptRequest.setCategory(request.getCategory());
    promptRequest.setIntentType(request.getIntentType());
    promptRequest.setRouteType(routeType);
    // [Evidence] 检索到的知识片段
    promptRequest.setKnowledgeList(context.getHitKnowledge());
    promptRequest.setHistory(request.getHistory());
    // [Realtime facts] 实时事实数据
    promptRequest.setRealtimeFacts(context.getRealtimeFacts());
    promptRequest.setContextState(request.getContextState());
    promptRequest.setCompareCandidates(compareCandidates);

    String prompt = promptBuilderService.buildPrompt(promptRequest);
    try {
      log.info("Proceeding to LLM invocation, sessionId={}, intentType={}, category={}, productId={}, confidence={}, hitKnowledgeCount={}",
        request.getSession().getSessionId(), request.getIntentType(), request.getCategory(),
        request.getCurrentProductId(), context.getConfidence(), context.getHitKnowledge().size());
      return buildResult(
        chatModelClient.chat(prompt),
        context.getHitKnowledge(),
        context.getConfidence(),
        null,
        AnswerPolicy.RAG_MODEL,
        request,
        routeType,
        compareCandidates
      );
    } catch (ModelInvokeException ex) {
      knowledgeStore.incrementModelFailures();
      log.warn("Chat model unavailable, sessionId={}, reason={}", request.getSession().getSessionId(), ex.getMessage(), ex);
      context.markFallback(FALLBACK_MODEL_UNAVAILABLE, ex.getMessage());
      return buildFallbackResult(context);
    }
  }

  /**
   * 构建知识路由上下文，包含向量检索、知识库就绪检查、证据收集等
   */
  private RouteKnowledgeContext buildRouteKnowledgeContext(ChatRouteRequest request, String routeType,
                                                           boolean includeRealtimeFacts, List<ProductCandidateVO> compareCandidates) {
    RouteKnowledgeContext context = new RouteKnowledgeContext(request, routeType, compareCandidates);
    context.setRealtimeFacts(includeRealtimeFacts ? buildRealtimeFacts(request) : "");

    // 检查知识库是否就绪
    if (!knowledgeStore.isKnowledgeReady()) {
      log.warn("Knowledge base is not ready, sessionId={}, routeType={}",
        request.getSession().getSessionId(), routeType);
      context.markFallback(FALLBACK_KNOWLEDGE_NOT_READY, "Knowledge base is not ready for retrieval");
    }

    if (context.hasFallback()) {
      // 已降级，但仍需补全当前商品知识（用于产品事实/对比场景）
      if (request.getIntentType() == QuestionIntentType.PRODUCT_FACT
        || request.getIntentType() == QuestionIntentType.COMPARE_RECOMMENDATION) {
        appendCurrentProductKnowledgeIfNecessary(context.getHitKnowledge(), request);
        if (!compareCandidates.isEmpty()) {
          appendCompareCandidateKnowledge(context.getHitKnowledge(), compareCandidates, request.getCurrentProductId());
        }
      }
      context.setHitKnowledge(curateKnowledge(request, context.getHitKnowledge(), compareCandidates));
      context.setConfidence(context.getHitKnowledge().stream().mapToDouble(RelatedKnowledgeVO::getScore).max().orElse(0d));
      return context;
    }

    // 向量化改写后的问题
    EmbeddingClient.EmbeddingResult embeddingResult = embeddingClient.embedSafely(request.getRewrittenQuestion());
    context.setEmbeddingResult(embeddingResult);
    if (!embeddingResult.isSuccess()) {
      log.warn("Embedding unavailable, sessionId={}, reason={}, message={}",
        request.getSession().getSessionId(), embeddingResult.getReason(), embeddingResult.getMessage(), embeddingResult.getError());
      context.markFallback(FALLBACK_EMBEDDING_UNAVAILABLE, embeddingResult.getMessage());
    } else {
      try {
        context.setHitKnowledge(retrieveKnowledge(request, compareCandidates, embeddingResult));
      } catch (Exception ex) {
        log.warn("Knowledge retrieval failed, sessionId={}, reason={}",
          request.getSession().getSessionId(), ex.getMessage(), ex);
        context.markFallback(FALLBACK_RETRIEVAL_UNAVAILABLE, ex.getMessage());
      }
    }

    // 补充当前商品知识和对比商品知识
    if (request.getIntentType() == QuestionIntentType.PRODUCT_FACT
      || request.getIntentType() == QuestionIntentType.COMPARE_RECOMMENDATION) {
      appendCurrentProductKnowledgeIfNecessary(context.getHitKnowledge(), request);
      if (!compareCandidates.isEmpty()) {
        appendCompareCandidateKnowledge(context.getHitKnowledge(), compareCandidates, request.getCurrentProductId());
      }
    }

    context.setHitKnowledge(curateKnowledge(request, context.getHitKnowledge(), compareCandidates));
    context.setConfidence(context.getHitKnowledge().stream().mapToDouble(RelatedKnowledgeVO::getScore).max().orElse(0d));

    // 证据不足时标记降级，不调用大模型
    if (!context.hasFallback() && !hasReliableEvidence(request, context.getHitKnowledge(), context.getConfidence(),
      context.getRealtimeFacts(), compareCandidates)) {
      log.info("Skip LLM invocation due to insufficient evidence, sessionId={}, intentType={}, productId={}, confidence={}, hitKnowledgeCount={}",
        request.getSession().getSessionId(), request.getIntentType(), request.getCurrentProductId(),
        context.getConfidence(), context.getHitKnowledge().size());
      knowledgeStore.incrementNoResult();
      context.markFallback(FALLBACK_NO_RELEVANT_KNOWLEDGE, "No reliable evidence for model invocation");
    }
    return context;
  }

  /**
   * 调用知识检索服务获取相关片段
   */
  private List<RelatedKnowledgeVO> retrieveKnowledge(ChatRouteRequest request, List<ProductCandidateVO> compareCandidates,
                                                     EmbeddingClient.EmbeddingResult embeddingResult) {
    KnowledgeRetrieveRequest query = new KnowledgeRetrieveRequest();
    query.setQuestion(request.getQuestion());
    query.setRewrittenQuestion(request.getRewrittenQuestion());
    query.setQuestionEmbedding(embeddingResult.getVector());
    query.setCategory(request.getCategory());
    query.setIntentType(request.getIntentType());
    query.setCurrentProductId(request.getCurrentProductId());
    query.setCurrentProductName(request.getCurrentProductName());
    query.setCompareCandidateIds(compareCandidates.stream().map(ProductCandidateVO::getProductId).toList());
    return new ArrayList<>(knowledgeRetrievalService.retrieve(query));
  }

  /**
   * 判断是否满足调用大模型的条件
   */
  private boolean canInvokeModel(RouteKnowledgeContext context) {
    return context != null && !context.hasFallback();
  }

  /**
   * 判断是否有足够可靠的证据进行回答
   */
  private boolean hasReliableEvidence(ChatRouteRequest request, List<RelatedKnowledgeVO> hitKnowledge, double confidence,
                                      String realtimeFacts, List<ProductCandidateVO> compareCandidates) {
    if (request.getIntentType() == QuestionIntentType.REALTIME_STATUS) {
      return StringUtils.hasText(realtimeFacts) || !hitKnowledge.isEmpty();
    }
    if (request.getIntentType() == QuestionIntentType.POLICY_QA) {
      return !hitKnowledge.isEmpty() && confidence >= aiProperties.getMinConfidence();
    }
    if (request.getIntentType() == QuestionIntentType.COMPARE_RECOMMENDATION) {
      return hasMatchedCurrentProduct(request.getCurrentProductId(), hitKnowledge)
        || (!hitKnowledge.isEmpty() && !compareCandidates.isEmpty());
    }
    if (request.getIntentType() == QuestionIntentType.PRODUCT_FACT) {
      if (request.getCurrentProductId() != null) {
        return hasMatchedCurrentProduct(request.getCurrentProductId(), hitKnowledge);
      }
      return !hitKnowledge.isEmpty() && confidence >= aiProperties.getMinConfidence();
    }
    return false;
  }

  /**
   * 知识片段精选：去重、范围过滤、截断
   */
  private List<RelatedKnowledgeVO> curateKnowledge(ChatRouteRequest request, List<RelatedKnowledgeVO> hitKnowledge,
                                                   List<ProductCandidateVO> compareCandidates) {
    if (hitKnowledge == null || hitKnowledge.isEmpty()) {
      return List.of();
    }

    Map<String, RelatedKnowledgeVO> deduplicated = new LinkedHashMap<>();
    for (RelatedKnowledgeVO item : hitKnowledge) {
      if (item == null || !StringUtils.hasText(item.getTitle()) || !StringUtils.hasText(item.getSourceType())) {
        continue;
      }
      if (!matchesKnowledgeScope(request, item, compareCandidates)) {
        continue;
      }
      String key = item.getSourceType() + "|" + item.getSourceId() + "|" + item.getTitle();
      deduplicated.putIfAbsent(key, item);
    }

    return deduplicated.values().stream()
      .sorted(Comparator.comparingDouble(RelatedKnowledgeVO::getScore).reversed())
      .limit(resolveKnowledgeLimit(request.getIntentType()))
      .toList();
  }

  /**
   * 判断知识片段是否属于当前意图允许的范围
   */
  private boolean matchesKnowledgeScope(ChatRouteRequest request, RelatedKnowledgeVO knowledge,
                                        List<ProductCandidateVO> compareCandidates) {
    if (knowledge.isRealtime()) {
      return true;
    }
    return switch (request.getIntentType()) {
      case PRODUCT_FACT -> isCurrentProductKnowledge(request.getCurrentProductId(), knowledge)
        || (request.getCurrentProductId() == null && isProductKnowledge(knowledge));
      case REALTIME_STATUS -> isCurrentProductKnowledge(request.getCurrentProductId(), knowledge)
        || "RULE".equalsIgnoreCase(knowledge.getSourceType());
      case POLICY_QA -> "RULE".equalsIgnoreCase(knowledge.getSourceType());
      case COMPARE_RECOMMENDATION -> isCurrentProductKnowledge(request.getCurrentProductId(), knowledge)
        || compareCandidates.stream().map(ProductCandidateVO::getProductId).map(String::valueOf)
        .anyMatch(candidateId -> candidateId.equals(knowledge.getSourceId()) && isProductKnowledge(knowledge));
      default -> false;
    };
  }

  /**
   * 根据意图类型决定检索知识片段的数量上限
   */
  private int resolveKnowledgeLimit(QuestionIntentType intentType) {
    return switch (intentType) {
      case PRODUCT_FACT -> 3;
      case REALTIME_STATUS -> 4;
      case POLICY_QA -> 2;
      case COMPARE_RECOMMENDATION -> 6;
      default -> aiProperties.getRetrievalTopK();
    };
  }

  /**
   * 检查检索结果中是否包含当前商品的知识
   */
  private boolean hasMatchedCurrentProduct(Long productId, List<RelatedKnowledgeVO> hitKnowledge) {
    if (productId == null || hitKnowledge == null || hitKnowledge.isEmpty()) {
      return false;
    }
    return hitKnowledge.stream()
      .anyMatch(item -> String.valueOf(productId).equals(item.getSourceId()) && isProductKnowledge(item));
  }

  /**
   * 若检索结果中缺少当前商品知识，则主动补充
   */
  private void appendCurrentProductKnowledgeIfNecessary(List<RelatedKnowledgeVO> hitKnowledge, ChatRouteRequest request) {
    Long productId = request.getCurrentProductId();
    if (productId == null || hasMatchedCurrentProduct(productId, hitKnowledge)) {
      return;
    }

    CurrentProductSnapshot snapshot = resolveCurrentProductSnapshot(request);
    if (snapshot != null) {
      hitKnowledge.add(currentProductKnowledge(snapshot.sourceType(), String.valueOf(snapshot.id()),
        snapshot.name(), snapshot.retrievalText(), 0.72d));
    }
  }

  /**
   * 补充对比候选商品的知识
   */
  private void appendCompareCandidateKnowledge(List<RelatedKnowledgeVO> hitKnowledge, List<ProductCandidateVO> compareCandidates,
                                               Long currentProductId) {
    for (ProductCandidateVO candidate : compareCandidates) {
      if (candidate == null || Objects.equals(candidate.getProductId(), currentProductId)) {
        continue;
      }
      boolean exists = hitKnowledge.stream()
        .anyMatch(item -> String.valueOf(candidate.getProductId()).equals(item.getSourceId()) && isProductKnowledge(item));
      if (exists) {
        continue;
      }
      if ("seckill".equalsIgnoreCase(candidate.getProductType())) {
        SeckillKnowledgeDTO seckill = seckillKnowledgeClient.getProductById(candidate.getProductId());
        if (seckill != null) {
          hitKnowledge.add(currentProductKnowledge("SECKILL", String.valueOf(seckill.getId()), seckill.getName(), seckill.toRetrievalText(), Math.max(0.55d, candidate.getScore())));
        }
      } else {
        ProductKnowledgeDTO product = productKnowledgeClient.getProductById(candidate.getProductId());
        if (product != null) {
          hitKnowledge.add(currentProductKnowledge("PRODUCT", String.valueOf(product.getId()), product.getName(), product.toRetrievalText(), Math.max(0.55d, candidate.getScore())));
        }
      }
    }
  }

  /**
   * 为对比推荐场景发现候选商品（同品类下其他商品）
   */
  private List<ProductCandidateVO> discoverCompareCandidates(ChatRouteRequest request) {
    CurrentProductSnapshot currentSnapshot = resolveCurrentProductSnapshot(request);
    if (currentSnapshot == null) {
      return List.of();
    }

    request.setCurrentProductName(currentSnapshot.name());
    request.setCurrentProductType("SECKILL".equalsIgnoreCase(currentSnapshot.sourceType()) ? PRODUCT_TYPE_SECKILL : PRODUCT_TYPE_NORMAL);

    if ("PRODUCT".equalsIgnoreCase(currentSnapshot.sourceType())) {
      ProductKnowledgeDTO currentProduct = productKnowledgeClient.getProductById(currentSnapshot.id());
      if (currentProduct == null) {
        return List.of();
      }
      return productKnowledgeClient.getAllProducts().stream()
        .filter(item -> item.getId() != null && !Objects.equals(item.getId(), currentProduct.getId()))
        .filter(item -> currentProduct.getCategoryId() == null || Objects.equals(item.getCategoryId(), currentProduct.getCategoryId()))
        .map(item -> toCandidate(item, currentProduct))
        .sorted(Comparator.comparingDouble(ProductCandidateVO::getScore).reversed())
        .limit(3)
        .toList();
    }

    SeckillKnowledgeDTO currentSeckill = seckillKnowledgeClient.getProductById(currentSnapshot.id());
    if (currentSeckill == null) {
      return List.of();
    }
    return seckillKnowledgeClient.getAllProducts().stream()
      .filter(item -> item.getId() != null && !Objects.equals(item.getId(), currentSeckill.getId()))
      .map(item -> toCandidate(item, currentSeckill))
      .sorted(Comparator.comparingDouble(ProductCandidateVO::getScore).reversed())
      .limit(3)
      .toList();
  }

  /**
   * 商品发现主逻辑：基于关键词检索，若无结果则返回热门推荐
   */
  private List<ProductCandidateVO> discoverProducts(ChatRouteRequest request) {
    int limit = 6;
    if (isBroadDiscoveryQuestion(request.getQuestion())) {
      return mergeAndRankBroadDiscovery(limit);
    }

    List<String> keywords = extractDiscoveryKeywords(request.getQuestion());
    if (keywords.isEmpty()) {
      return mergeAndRankBroadDiscovery(limit);
    }

    Map<String, ProductCandidateVO> candidates = new LinkedHashMap<>();
    for (String keyword : keywords) {
      for (ProductKnowledgeDTO product : productKnowledgeClient.searchProducts(keyword)) {
        ProductCandidateVO candidate = toDiscoveryCandidate(product, keyword, "normal");
        mergeDiscoveryCandidate(candidates, candidate, limit);
      }
      for (SeckillKnowledgeDTO product : seckillKnowledgeClient.searchProducts(keyword)) {
        ProductCandidateVO candidate = toDiscoveryCandidate(product, keyword, "seckill");
        mergeDiscoveryCandidate(candidates, candidate, limit);
      }
      if (candidates.size() >= limit * 2) {
        break;
      }
    }
    appendLocalDiscoveryCandidates(candidates, keywords, limit);

    return candidates.values().stream()
      .sorted(Comparator.comparingDouble(ProductCandidateVO::getScore).reversed())
      .limit(limit)
      .toList();
  }

  /**
   * 宽泛商品发现：返回全量商品并排序截断
   */
  private List<ProductCandidateVO> mergeAndRankBroadDiscovery(int limit) {
    List<ProductCandidateVO> candidates = new ArrayList<>();
    for (ProductKnowledgeDTO product : productKnowledgeClient.getAllProducts()) {
      ProductCandidateVO candidate = toDiscoveryCandidate(product, "", "normal");
      if (candidate != null) {
        candidates.add(candidate);
      }
    }
    for (SeckillKnowledgeDTO product : seckillKnowledgeClient.getAllProducts()) {
      ProductCandidateVO candidate = toDiscoveryCandidate(product, "", "seckill");
      if (candidate != null) {
        candidates.add(candidate);
      }
    }
    return candidates.stream()
      .sorted(Comparator.comparing(ProductCandidateVO::getProductType)
        .thenComparing(ProductCandidateVO::getName))
      .limit(limit)
      .toList();
  }

  /**
   * 合并候选商品（保留得分更高的）
   */
  private void mergeDiscoveryCandidate(Map<String, ProductCandidateVO> candidates, ProductCandidateVO candidate, int limit) {
    if (candidate == null || candidates.size() > limit * 2) {
      return;
    }
    String key = candidate.getProductType() + ":" + candidate.getProductId();
    ProductCandidateVO existing = candidates.get(key);
    if (existing == null || candidate.getScore() > existing.getScore()) {
      candidates.put(key, candidate);
    }
  }

  /**
   * 本地兜底匹配：遍历全量商品计算相似度得分
   */
  private void appendLocalDiscoveryCandidates(Map<String, ProductCandidateVO> candidates, List<String> keywords, int limit) {
    for (ProductKnowledgeDTO product : productKnowledgeClient.getAllProducts()) {
      double score = localDiscoveryScore(keywords, product.getName(), product.getSubtitle(), product.getDetail());
      if (score <= 0d) {
        continue;
      }
      ProductCandidateVO candidate = toDiscoveryCandidate(product, "", "normal");
      if (candidate != null) {
        candidate.setScore(Math.max(candidate.getScore(), score));
        mergeDiscoveryCandidate(candidates, candidate, limit);
      }
    }
    for (SeckillKnowledgeDTO product : seckillKnowledgeClient.getAllProducts()) {
      double score = localDiscoveryScore(keywords, product.getName(), product.toRetrievalText());
      if (score <= 0d) {
        continue;
      }
      ProductCandidateVO candidate = toDiscoveryCandidate(product, "", "seckill");
      if (candidate != null) {
        candidate.setScore(Math.max(candidate.getScore(), score));
        mergeDiscoveryCandidate(candidates, candidate, limit);
      }
    }
  }

  private ProductCandidateVO toDiscoveryCandidate(ProductKnowledgeDTO product, String keyword, String type) {
    if (product == null || product.getId() == null || !StringUtils.hasText(product.getName())) {
      return null;
    }
    ProductCandidateVO candidate = new ProductCandidateVO();
    candidate.setProductId(product.getId());
    candidate.setProductType(type);
    candidate.setName(product.getName());
    candidate.setSubtitle(product.getSubtitle());
    candidate.setPrice(product.getPrice());
    candidate.setScore(discoveryScore(keyword, product.getName()));
    return candidate;
  }

  private ProductCandidateVO toDiscoveryCandidate(SeckillKnowledgeDTO product, String keyword, String type) {
    if (product == null || product.getId() == null || !StringUtils.hasText(product.getName())) {
      return null;
    }
    ProductCandidateVO candidate = new ProductCandidateVO();
    candidate.setProductId(product.getId());
    candidate.setProductType(type);
    candidate.setName(product.getName());
    candidate.setPrice(product.getSeckillPrice() != null ? product.getSeckillPrice() : product.getPrice());
    candidate.setScore(discoveryScore(keyword, product.getName()));
    return candidate;
  }

  /**
   * 计算关键词与商品名的匹配得分（基于完全匹配、包含、分词重叠）
   */
  private double discoveryScore(String keyword, String name) {
    String normalizedKeyword = compact(keyword);
    String normalizedName = compact(name);
    if (!StringUtils.hasText(normalizedKeyword)) {
      return 0.7d;
    }
    if (!StringUtils.hasText(normalizedName)) {
      return 0d;
    }
    if (normalizedKeyword.equals(normalizedName)) {
      return 1d;
    }
    if (normalizedName.contains(normalizedKeyword)) {
      return 0.96d;
    }
    if (normalizedKeyword.contains(normalizedName)) {
      return 0.90d;
    }
    int overlap = 0;
    for (String token : splitTokens(keyword)) {
      String compactToken = compact(token);
      if (compactToken.length() >= 2 && normalizedName.contains(compactToken)) {
        overlap++;
      }
    }
    return overlap == 0 ? 0d : Math.min(0.88d, 0.55d + overlap * 0.12d);
  }

  /**
   * 本地匹配得分（遍历多个文本字段）
   */
  private double localDiscoveryScore(List<String> keywords, String... texts) {
    double bestScore = 0d;
    for (String keyword : keywords) {
      String normalizedKeyword = compact(keyword);
      if (!StringUtils.hasText(normalizedKeyword)) {
        continue;
      }
      for (String text : texts) {
        String normalizedText = compact(text);
        if (!StringUtils.hasText(normalizedText)) {
          continue;
        }
        if (normalizedText.contains(normalizedKeyword)) {
          bestScore = Math.max(bestScore, 0.9d);
          continue;
        }
        for (String token : splitTokens(keyword)) {
          String compactToken = compact(token);
          if (compactToken.length() >= 2 && normalizedText.contains(compactToken)) {
            bestScore = Math.max(bestScore, 0.72d);
          }
        }
      }
    }
    return bestScore;
  }

  /**
   * 从用户问题中提取商品发现关键词
   */
  private List<String> extractDiscoveryKeywords(String question) {
    LinkedHashSet<String> keywords = new LinkedHashSet<>();
    String normalized = normalizeDiscovery(question);
    if (!StringUtils.hasText(normalized)) {
      return List.of();
    }

    String cleaned = normalized;
    for (String phrase : List.of("this product", "that product", "this item", "recommend", "find", "search", "what products", "what to buy", "mall")) {
      cleaned = cleaned.replace(phrase, " ");
    }
    cleaned = collapseSpaces(cleaned);
    if (StringUtils.hasText(cleaned)) {
      keywords.add(cleaned);
    }

    String stripped = cleaned;
    for (String word : List.of("show", "look", "want", "need", "buy", "product", "products", "item", "items", "mall")) {
      stripped = stripped.replace(word, " ");
    }
    stripped = collapseSpaces(stripped);
    if (StringUtils.hasText(stripped)) {
      keywords.add(stripped);
    }

    for (String token : splitTokens(stripped)) {
      if (token.length() >= 2) {
        keywords.add(token);
      }
    }
    for (String token : splitTokens(cleaned)) {
      if (token.length() >= 2) {
        keywords.add(token);
      }
    }
    return keywords.stream().filter(StringUtils::hasText).limit(4).toList();
  }

  /**
   * 判断是否为宽泛的商品发现问题（如“有什么商品”、“推荐”）
   */
  private boolean isBroadDiscoveryQuestion(String question) {
    String normalized = normalizeDiscovery(question);
    return normalized.contains("what products")
      || normalized.contains("show products")
      || normalized.contains("recommend")
      || normalized.contains("search")
      || normalized.equals("mall")
      || normalized.equals("products");
  }

  /**
   * 构建商品发现场景的回答文案
   */
  private String buildDiscoveryAnswer(String question, List<ProductCandidateVO> candidates, boolean broadQuestion) {
    StringBuilder builder = new StringBuilder();
    if (broadQuestion) {
      builder.append("I found these products that may match your request: ");
    } else {
      builder.append("I found these candidate products based on your question: ");
    }
    for (int i = 0; i < candidates.size(); i++) {
      ProductCandidateVO candidate = candidates.get(i);
      if (i > 0) {
        builder.append("; ");
      }
      builder.append(candidate.getName());
      if (candidate.getPrice() != null) {
        builder.append(" (price: ").append(candidate.getPrice());
        if ("seckill".equalsIgnoreCase(candidate.getProductType())) {
          builder.append(", seckill");
        }
        builder.append(")");
      } else if ("seckill".equalsIgnoreCase(candidate.getProductType())) {
        builder.append(" (seckill)");
      }
    }
    builder.append(" If you want, I can continue comparing them or explain the top recommendation.");
    return builder.toString();
  }

  /**
   * 构建无精确匹配时的兜底回答
   */
  private String buildDiscoveryFallbackAnswer(String question, List<ProductCandidateVO> broadCandidates) {
    StringBuilder builder = new StringBuilder("I could not find an exact match for your question");
    if (StringUtils.hasText(question)) {
      builder.append(": ").append(question.trim());
    }
    builder.append(". Here are some broader candidate products: ");
    for (int i = 0; i < broadCandidates.size(); i++) {
      ProductCandidateVO candidate = broadCandidates.get(i);
      if (i > 0) {
        builder.append("; ");
      }
      builder.append(candidate.getName());
      if (candidate.getPrice() != null) {
        builder.append(" (price: ").append(candidate.getPrice());
        if ("seckill".equalsIgnoreCase(candidate.getProductType())) {
          builder.append(", seckill");
        }
        builder.append(")");
      } else if ("seckill".equalsIgnoreCase(candidate.getProductType())) {
        builder.append(" (seckill)");
      }
    }
    builder.append(" You can provide a more specific product name, brand, or category and I will narrow it down.");
    return builder.toString();
  }

  private ChatRouteResult buildDiscoveryResult(String answer, List<ProductCandidateVO> candidates,
                                               double confidence, String fallbackReason, ChatRouteRequest request) {
    ChatRouteResult result = new ChatRouteResult();
    result.setAnswer(answer);
    // sources 鍙〃绀鸿瘉鎹潵婧愶紝涓嶅啀鎶婂€欓€夊晢鍝佸悕鍚堝埌 sources 閲屻€?
    // 鍟嗗搧鍙戠幇/鍊欓€夊垪琛ㄤ俊鎭崟鐙蛋 compareCandidates 瀛楁銆?
    result.setSources(List.of());
    result.setHitKnowledge(List.of());
    result.setConfidence(confidence);
    result.setFallbackReason(fallbackReason);
    result.setAnswerPolicy(AnswerPolicy.DISCOVERY_ONLY);
    result.setCategory(QuestionCategory.PRODUCT_DISCOVERY);
    result.setIntentType(QuestionIntentType.PRODUCT_DISCOVERY);
    result.setRouteType(ROUTE_DISCOVERY);
    result.setRewrittenQuestion(request.getRewrittenQuestion());
    result.setContextState(request.getContextState());
    result.setCompareCandidates(candidates);
    return result;
  }

  private ProductCandidateVO toCandidate(ProductKnowledgeDTO candidate, ProductKnowledgeDTO currentProduct) {
    ProductCandidateVO vo = new ProductCandidateVO();
    vo.setProductId(candidate.getId());
    vo.setProductType("normal");
    vo.setName(candidate.getName());
    vo.setSubtitle(candidate.getSubtitle());
    vo.setPrice(candidate.getPrice());
    vo.setScore(scoreCandidate(candidate.getName(), currentProduct.getName(), candidate.getPrice(), currentProduct.getPrice()));
    return vo;
  }

  private ProductCandidateVO toCandidate(SeckillKnowledgeDTO candidate, SeckillKnowledgeDTO currentProduct) {
    ProductCandidateVO vo = new ProductCandidateVO();
    vo.setProductId(candidate.getId());
    vo.setProductType("seckill");
    vo.setName(candidate.getName());
    vo.setPrice(candidate.getSeckillPrice() != null ? candidate.getSeckillPrice() : candidate.getPrice());
    BigDecimal currentPrice = currentProduct.getSeckillPrice() != null ? currentProduct.getSeckillPrice() : currentProduct.getPrice();
    vo.setScore(scoreCandidate(candidate.getName(), currentProduct.getName(), vo.getPrice(), currentPrice));
    return vo;
  }

  /**
   * 计算对比候选商品的得分（基于名称关键词重叠和价格相近程度）
   */
  private double scoreCandidate(String candidateName, String currentName, BigDecimal candidatePrice, BigDecimal currentPrice) {
    double score = shareKeyword(candidateName, currentName) ? 0.6d : 0.2d;
    if (candidatePrice != null && currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal ratio = candidatePrice.subtract(currentPrice).abs()
        .divide(currentPrice, 4, java.math.RoundingMode.HALF_UP);
      if (ratio.doubleValue() <= 0.3d) {
        score += 0.3d;
      } else if (ratio.doubleValue() <= 0.5d) {
        score += 0.15d;
      }
    }
    return Math.min(0.95d, score);
  }

  private boolean shareKeyword(String left, String right) {
    String normalizedLeft = compact(left);
    String normalizedRight = compact(right);
    if (!StringUtils.hasText(normalizedLeft) || !StringUtils.hasText(normalizedRight)) {
      return false;
    }
    return normalizedLeft.contains(normalizedRight) || normalizedRight.contains(normalizedLeft)
      || tokens(normalizedLeft).stream().anyMatch(token -> token.length() >= 2 && normalizedRight.contains(token));
  }

  private List<String> tokens(String text) {
    return List.of(text.split("\\s+"));
  }

  private List<String> splitTokens(String text) {
    return tokens(normalizeDiscovery(text)).stream()
      .map(this::collapseSpaces)
      .filter(StringUtils::hasText)
      .toList();
  }

  private String compact(String text) {
    return text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\u4e00-\\u9fa5]+", " ");
  }

  private String normalizeDiscovery(String text) {
    return collapseSpaces(compact(text));
  }

  private String collapseSpaces(String text) {
    if (!StringUtils.hasText(text)) {
      return "";
    }
    return text.trim().replaceAll("\\s+", " ");
  }

  /**
   * 构建实时信息文本（价格、库存、秒杀状态等）
   */
  private String buildRealtimeFacts(ChatRouteRequest request) {
    if ((request.getCategory() != QuestionCategory.REALTIME_STATUS && request.getCategory() != QuestionCategory.ACTIVITY_RULE)
      || request.getCurrentProductId() == null) {
      return "";
    }

    CurrentProductSnapshot snapshot = resolveCurrentProductSnapshot(request);
    return snapshot == null ? "" : snapshot.realtimeFacts();
  }

  /**
   * 根据上下文构建降级结果（分级兜底）
   */
  private ChatRouteResult buildFallbackResult(RouteKnowledgeContext context) {
    FallbackDecision decision = buildCurrentProductDirectFallback(context);
    if (decision == null) {
      decision = buildRealtimeOnlyFallback(context);
    }
    if (decision == null) {
      decision = buildRuleBasedFallback(context);
    }
    if (decision == null) {
      decision = buildGenericFallback(context);
    }
    return buildResult(
      decision.answer(),
      decision.knowledge(),
      context.getConfidence(),
      context.getFallbackReason(),
      decision.answerPolicy(),
      context.getRequest(),
      context.getRouteType(),
      context.getCompareCandidates()
    );
  }

  /**
   * 基于当前锁定商品直接回答（不使用检索结果）
   */
  private FallbackDecision buildCurrentProductDirectFallback(RouteKnowledgeContext context) {
    if (context.getRequest().getCurrentProductId() == null) {
      return null;
    }
    if (context.getRequest().getIntentType() == QuestionIntentType.POLICY_QA) {
      return null;
    }

    CurrentProductSnapshot snapshot = resolveCurrentProductSnapshot(context.getRequest());
    if (snapshot == null) {
      return null;
    }

    RelatedKnowledgeVO evidence = currentProductKnowledge(
      snapshot.sourceType(),
      String.valueOf(snapshot.id()),
      snapshot.name(),
      snapshot.retrievalText(),
      Math.max(0.72d, context.getConfidence())
    );
    return new FallbackDecision(
      renderCurrentProductDirectAnswer(snapshot.name(), snapshot.retrievalText(), snapshot.realtimeFacts(), context.getCompareCandidates()),
      mergeFallbackKnowledge(context.getHitKnowledge(), List.of(evidence)),
      resolveFallbackPolicy(context)
    );
  }

  /**
   * 仅依赖实时数据回答（不调用大模型）
   */
  private FallbackDecision buildRealtimeOnlyFallback(RouteKnowledgeContext context) {
    if (!StringUtils.hasText(context.getRealtimeFacts())) {
      return null;
    }
    String answer = context.getRealtimeFacts().trim() + "\n以上内容直接来自实时数据，当前未使用向量检索结果。";
    return new FallbackDecision(answer, context.getHitKnowledge(), AnswerPolicy.REALTIME_ONLY);
  }

  /**
   * 基于规则库片段进行保守回答
   */
  private FallbackDecision buildRuleBasedFallback(RouteKnowledgeContext context) {
    List<RelatedKnowledgeVO> ruleKnowledge = collectRuleFallbackKnowledge(context);
    if (ruleKnowledge.isEmpty()) {
      return null;
    }

    StringBuilder builder = new StringBuilder("根据当前可用规则信息，给你一个保守回答：\n");
    for (RelatedKnowledgeVO rule : ruleKnowledge) {
      builder.append("- ").append(summarizeSnippet(rule.getSnippet())).append("\n");
    }
    builder.append("如需最终确认，请以商品详情页、结算页或人工客服说明为准。");
    return new FallbackDecision(builder.toString().trim(), ruleKnowledge, resolveFallbackPolicy(context));
  }

  /**
   * 通用兜底回答（知识库未就绪、向量服务异常等）
   */
  private FallbackDecision buildGenericFallback(RouteKnowledgeContext context) {
    String answer;
    if (FALLBACK_KNOWLEDGE_NOT_READY.equals(context.getFallbackReason())) {
      answer = "知识库尚未就绪。你可以稍后重试，或从商品详情页继续提问，这样我会优先使用当前商品的上下文信息。";
    } else if (FALLBACK_EMBEDDING_UNAVAILABLE.equals(context.getFallbackReason())) {
      answer = "向量服务当前不可用。建议从商品详情页继续提问，或稍后重试。";
    } else if (FALLBACK_RETRIEVAL_UNAVAILABLE.equals(context.getFallbackReason())) {
      answer = "知识检索当前不可用。建议稍后重试，或者提供更明确的商品名称。";
    } else {
      answer = switch (context.getRequest().getIntentType()) {
        case COMPARE_RECOMMENDATION -> "当前证据不足，暂时无法给出可靠的对比建议。建议缩小对比范围，或直接从商品详情页继续提问。";
        case POLICY_QA -> "当前规则证据不足。建议以结算页说明或人工客服答复为准。";
        case REALTIME_STATUS -> "当前实时数据不可用。建议从商品详情页重试。";
        default -> "当前证据不足，暂时无法可靠回答。建议补充明确商品名，或从商品详情页继续提问。";
      };
    }
    return new FallbackDecision(answer, context.getHitKnowledge(), resolveFallbackPolicy(context));
  }

  /**
   * 收集规则类降级知识（优先使用检索到的规则片段，否则从内存中匹配）
   */
  private List<RelatedKnowledgeVO> collectRuleFallbackKnowledge(RouteKnowledgeContext context) {
    List<RelatedKnowledgeVO> directHits = context.getHitKnowledge().stream()
      .filter(item -> "RULE".equalsIgnoreCase(item.getSourceType()))
      .limit(2)
      .toList();
    if (!directHits.isEmpty()) {
      return directHits;
    }

    String lookup = compact(context.getRequest().getRewrittenQuestion() + " " + context.getRequest().getQuestion());
    return knowledgeStore.getAllChunks().stream()
      .filter(chunk -> chunk.getSourceType() != null && "RULE".equalsIgnoreCase(chunk.getSourceType().name()))
      .map(chunk -> {
        String haystack = compact(chunk.getTitle() + " " + chunk.getContent());
        double score = haystack.contains(lookup) ? 0.85d : fallbackTokenOverlapScore(lookup, haystack);
        return score <= 0d ? null : currentProductKnowledge("RULE", chunk.getSourceId(), chunk.getTitle(), chunk.getContent(), score);
      })
      .filter(Objects::nonNull)
      .sorted(Comparator.comparingDouble(RelatedKnowledgeVO::getScore).reversed())
      .limit(2)
      .toList();
  }

  private double fallbackTokenOverlapScore(String left, String right) {
    if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
      return 0d;
    }
    int overlap = 0;
    for (String token : left.split("\\s+")) {
      if (token.length() >= 2 && right.contains(token)) {
        overlap++;
      }
    }
    return overlap == 0 ? 0d : Math.min(0.75d, 0.35d + overlap * 0.12d);
  }

  private String renderCurrentProductDirectAnswer(String productName, String retrievalText, String realtimeFacts,
                                                  List<ProductCandidateVO> compareCandidates) {
    StringBuilder builder = new StringBuilder("根据当前锁定商品直接回答");
    if (StringUtils.hasText(productName)) {
      builder.append("：").append(productName.trim());
    }
    builder.append("\n");
    if (StringUtils.hasText(retrievalText)) {
      builder.append(summarizeSnippet(retrievalText)).append("\n");
    }
    if (StringUtils.hasText(realtimeFacts)) {
      builder.append("实时信息：\n").append(realtimeFacts.trim()).append("\n");
    }
    if (compareCandidates != null && !compareCandidates.isEmpty()) {
      builder.append("可对比商品：")
        .append(String.join("、", compareCandidates.stream().map(ProductCandidateVO::getName)
          .filter(StringUtils::hasText).distinct().limit(3).toList()))
        .append("\n");
    }
    builder.append("当前回答未使用向量检索结果。");
    return builder.toString().trim();
  }

  private String summarizeSnippet(String snippet) {
    if (!StringUtils.hasText(snippet)) {
      return "";
    }
    String normalized = snippet.replace('\n', ' ').trim();
    return normalized.length() <= 220 ? normalized : normalized.substring(0, 220) + "...";
  }

  private List<RelatedKnowledgeVO> mergeFallbackKnowledge(List<RelatedKnowledgeVO> primary, List<RelatedKnowledgeVO> extra) {
    Map<String, RelatedKnowledgeVO> merged = new LinkedHashMap<>();
    for (RelatedKnowledgeVO item : primary) {
      merged.put(item.getSourceType() + "|" + item.getSourceId() + "|" + item.getTitle(), item);
    }
    for (RelatedKnowledgeVO item : extra) {
      merged.putIfAbsent(item.getSourceType() + "|" + item.getSourceId() + "|" + item.getTitle(), item);
    }
    return new ArrayList<>(merged.values());
  }

  private AnswerPolicy resolveFallbackPolicy(RouteKnowledgeContext context) {
    return FALLBACK_MODEL_UNAVAILABLE.equals(context.getFallbackReason())
      ? AnswerPolicy.RAG_FALLBACK_MODEL_ERROR
      : AnswerPolicy.RAG_FALLBACK_NO_KNOWLEDGE;
  }

  private RelatedKnowledgeVO currentProductKnowledge(String sourceType, String sourceId, String title, String snippet, double score) {
    RelatedKnowledgeVO knowledge = new RelatedKnowledgeVO();
    knowledge.setDocumentId(sourceType.toLowerCase(Locale.ROOT) + "-current-" + sourceId);
    knowledge.setTitle(title);
    knowledge.setSourceType(sourceType);
    knowledge.setSourceId(sourceId);
    knowledge.setSnippet(snippet);
    knowledge.setScore(score);
    knowledge.setRealtime(false);
    return knowledge;
  }

  private boolean isCurrentProductKnowledge(Long productId, RelatedKnowledgeVO knowledge) {
    return productId != null && String.valueOf(productId).equals(knowledge.getSourceId()) && isProductKnowledge(knowledge);
  }

  private boolean isProductKnowledge(RelatedKnowledgeVO knowledge) {
    return "PRODUCT".equalsIgnoreCase(knowledge.getSourceType())
      || "SECKILL".equalsIgnoreCase(knowledge.getSourceType());
  }

  /**
   * 解析当前锁定的商品快照（优先使用请求中携带的类型/名称）
   */
  private CurrentProductSnapshot resolveCurrentProductSnapshot(ChatRouteRequest request) {
    Long productId = request.getCurrentProductId();
    if (productId == null) {
      return null;
    }

    String preferredType = request.getCurrentProductType();
    String preferredName = request.getCurrentProductName();

    if (PRODUCT_TYPE_SECKILL.equalsIgnoreCase(preferredType)) {
      CurrentProductSnapshot snapshot = loadSeckillSnapshot(productId);
      if (snapshot != null) {
        return snapshot;
      }
    }
    if (PRODUCT_TYPE_NORMAL.equalsIgnoreCase(preferredType)) {
      CurrentProductSnapshot snapshot = loadNormalSnapshot(productId);
      if (snapshot != null) {
        return snapshot;
      }
    }

    CurrentProductSnapshot normalSnapshot = loadNormalSnapshot(productId);
    CurrentProductSnapshot seckillSnapshot = loadSeckillSnapshot(productId);

    if (StringUtils.hasText(preferredName)) {
      if (matchesProductName(preferredName, normalSnapshot == null ? null : normalSnapshot.name())) {
        return normalSnapshot;
      }
      if (matchesProductName(preferredName, seckillSnapshot == null ? null : seckillSnapshot.name())) {
        return seckillSnapshot;
      }
    }

    return normalSnapshot != null ? normalSnapshot : seckillSnapshot;
  }

  private CurrentProductSnapshot loadNormalSnapshot(Long productId) {
    ProductKnowledgeDTO product = productKnowledgeClient.getProductById(productId);
    if (product == null || !StringUtils.hasText(product.getName())) {
      return null;
    }
    return new CurrentProductSnapshot(
      "PRODUCT",
      product.getId(),
      product.getName(),
      product.toRetrievalText(),
      product.toRealtimeFacts()
    );
  }

  private CurrentProductSnapshot loadSeckillSnapshot(Long productId) {
    SeckillKnowledgeDTO seckill = seckillKnowledgeClient.getProductById(productId);
    if (seckill == null || !StringUtils.hasText(seckill.getName())) {
      return null;
    }
    return new CurrentProductSnapshot(
      "SECKILL",
      seckill.getId(),
      seckill.getName(),
      seckill.toRetrievalText(),
      seckill.toRealtimeFacts()
    );
  }

  private boolean matchesProductName(String expectedName, String actualName) {
    String left = normalizeProductName(expectedName);
    String right = normalizeProductName(actualName);
    return StringUtils.hasText(left) && StringUtils.hasText(right)
      && (left.contains(right) || right.contains(left));
  }

  private String normalizeProductName(String productName) {
    return productName == null ? "" : productName.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
  }

  /**
   * 降级决策记录
   */
  private record FallbackDecision(String answer, List<RelatedKnowledgeVO> knowledge, AnswerPolicy answerPolicy) {
  }

  /**
   * 当前商品快照（用于兜底回答）
   */
  private record CurrentProductSnapshot(String sourceType, Long id, String name, String retrievalText, String realtimeFacts) {
  }

  /**
   * 知识路由上下文内部类，封装一次路由执行所需的状态
   */
  private static final class RouteKnowledgeContext {
    private final ChatRouteRequest request;
    private final String routeType;
    private final List<ProductCandidateVO> compareCandidates;
    private EmbeddingClient.EmbeddingResult embeddingResult;
    private List<RelatedKnowledgeVO> hitKnowledge = new ArrayList<>();
    private String realtimeFacts = "";
    private double confidence;
    private String fallbackReason;
    private String fallbackMessage;

    private RouteKnowledgeContext(ChatRouteRequest request, String routeType, List<ProductCandidateVO> compareCandidates) {
      this.request = request;
      this.routeType = routeType;
      this.compareCandidates = compareCandidates == null ? List.of() : compareCandidates;
    }

    public ChatRouteRequest getRequest() {
      return request;
    }

    public String getRouteType() {
      return routeType;
    }

    public List<ProductCandidateVO> getCompareCandidates() {
      return compareCandidates;
    }

    public EmbeddingClient.EmbeddingResult getEmbeddingResult() {
      return embeddingResult;
    }

    public void setEmbeddingResult(EmbeddingClient.EmbeddingResult embeddingResult) {
      this.embeddingResult = embeddingResult;
    }

    public List<RelatedKnowledgeVO> getHitKnowledge() {
      return hitKnowledge;
    }

    public void setHitKnowledge(List<RelatedKnowledgeVO> hitKnowledge) {
      this.hitKnowledge = hitKnowledge == null ? new ArrayList<>() : new ArrayList<>(hitKnowledge);
    }

    public String getRealtimeFacts() {
      return realtimeFacts;
    }

    public void setRealtimeFacts(String realtimeFacts) {
      this.realtimeFacts = realtimeFacts == null ? "" : realtimeFacts;
    }

    public double getConfidence() {
      return confidence;
    }

    public void setConfidence(double confidence) {
      this.confidence = confidence;
    }

    public String getFallbackReason() {
      return fallbackReason;
    }

    public String getFallbackMessage() {
      return fallbackMessage;
    }

    public boolean hasFallback() {
      return StringUtils.hasText(fallbackReason);
    }

    public void markFallback(String fallbackReason, String fallbackMessage) {
      this.fallbackReason = fallbackReason;
      this.fallbackMessage = fallbackMessage;
    }
  }

  /**
   * 构建固定话术结果（如欢迎语、拒答）
   */
  private ChatRouteResult fixedResult(String answer, QuestionCategory category, QuestionIntentType intentType,
                                      String routeType, String fallbackReason, AnswerPolicy answerPolicy,
                                      ChatRouteRequest request) {
    return buildResult(answer, List.of(), answerPolicy == AnswerPolicy.FIXED_TEMPLATE ? 1d : 0d,
      fallbackReason, answerPolicy, request, routeType, List.of(), category, intentType);
  }

  private ChatRouteResult buildResult(String answer, List<RelatedKnowledgeVO> hitKnowledge, double confidence,
                                      String fallbackReason, AnswerPolicy answerPolicy, ChatRouteRequest request,
                                      String routeType, List<ProductCandidateVO> compareCandidates) {
    return buildResult(answer, hitKnowledge, confidence, fallbackReason, answerPolicy,
      request, routeType, compareCandidates, request.getCategory(), request.getIntentType());
  }

  private ChatRouteResult buildResult(String answer, List<RelatedKnowledgeVO> hitKnowledge, double confidence,
                                      String fallbackReason, AnswerPolicy answerPolicy, ChatRouteRequest request,
                                      String routeType, List<ProductCandidateVO> compareCandidates,
                                      QuestionCategory category, QuestionIntentType intentType) {
    ChatRouteResult result = new ChatRouteResult();
    result.setAnswer(answer);
    result.setHitKnowledge(hitKnowledge);
    result.setSources(hitKnowledge.stream().map(RelatedKnowledgeVO::getTitle).distinct().toList());
    result.setConfidence(confidence);
    result.setFallbackReason(fallbackReason);
    result.setAnswerPolicy(answerPolicy);
    result.setCategory(category);
    result.setIntentType(intentType);
    result.setRouteType(routeType);
    result.setRewrittenQuestion(request.getRewrittenQuestion());
    result.setContextState(request.getContextState());
    result.setCompareCandidates(compareCandidates);
    return result;
  }
}
