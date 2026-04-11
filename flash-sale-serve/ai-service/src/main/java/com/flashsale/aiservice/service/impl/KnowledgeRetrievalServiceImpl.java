package com.flashsale.aiservice.service.impl;

import com.flashsale.aiservice.config.AiProperties;
import com.flashsale.aiservice.domain.enums.KnowledgeSourceType;
import com.flashsale.aiservice.domain.enums.QuestionCategory;
import com.flashsale.aiservice.domain.enums.QuestionIntentType;
import com.flashsale.aiservice.domain.po.KnowledgeChunkPO;
import com.flashsale.aiservice.domain.vo.RelatedKnowledgeVO;
import com.flashsale.aiservice.service.KnowledgeRetrievalService;
import com.flashsale.aiservice.service.VectorStoreService;
import com.flashsale.aiservice.service.model.KnowledgeRetrieveRequest;
import com.flashsale.aiservice.util.SimilarityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 知识检索服务实现类
 *
 * 核心职责：
 * 根据用户问题向量、意图分类、商品上下文等信息，从向量存储中检索出最相关的知识片段。
 *
 * 检索策略：
 * 1. 意图范围过滤：根据意图类型（产品事实、政策问答、对比推荐等）限定检索的知识来源。
 * 2. 类别过滤：进一步根据问题类别（售后、配送、活动规则）缩小范围。
 * 3. 混合评分：结合向量相似度和词汇匹配增强（标题命中、内容包含）计算最终得分。
 * 4. 上下文加权：对当前商品和对比候选商品给予额外加分，提升召回相关性。
 * 5. 兜底保留：即使得分较低，当前锁定的商品知识也会被强制保留。
 * 6. 数量截断：根据意图类型和类别动态决定返回的知识片段数量上限。
 *
 * 该类是 RAG 流程中连接“向量库”与“大模型”的关键桥梁。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalServiceImpl implements KnowledgeRetrievalService {

  // 词汇增强系数：标题精确命中问题的加分
  private static final double EXACT_TITLE_BOOST = 0.65d;
  // 词汇增强系数：标题与问题有分词重叠的加分
  private static final double PARTIAL_TITLE_BOOST = 0.25d;
  // 词汇增强系数：内容包含问题的加分
  private static final double CONTENT_BOOST = 0.10d;
  // 上下文加权：当前锁定商品的加分
  private static final double CURRENT_PRODUCT_BOOST = 0.35d;
  // 上下文加权：对比候选商品的加分
  private static final double COMPARE_CANDIDATE_BOOST = 0.18d;

  private final VectorStoreService vectorStoreService; // 向量存储服务
  private final AiProperties aiProperties;             // AI 配置属性

  /**
   * 执行知识检索
   *
   * 流程：
   * 1. 获取全量向量分块（从内存或向量库）。
   * 2. 按意图范围和问题类别进行过滤。
   * 3. 计算每个分块的得分（向量相似度 + 词汇增强 + 上下文加权）。
   * 4. 按得分降序排序，并截取指定数量。
   * 5. 返回结果列表。
   *
   * @param request 包含问题向量、意图类型、商品上下文等的检索请求
   * @return 相关知识片段列表（已排序并截断）
   */
  @Override
  public List<RelatedKnowledgeVO> retrieve(KnowledgeRetrieveRequest request) {
    // 越界话题不进行检索
    if (request == null || request.getCategory() == QuestionCategory.OUT_OF_SCOPE) {
      return List.of();
    }

    // 从向量存储获取所有分块，并进行过滤
    Stream<KnowledgeChunkPO> stream = vectorStoreService.allChunks().stream()
      .filter(chunk -> matchesIntentScope(chunk, request))  // 意图范围过滤
      .filter(chunk -> matchesCategory(chunk, request));    // 类别过滤

    return stream
      .map(chunk -> toKnowledge(chunk, request))           // 转换为 VO 并计算得分
      .filter(item -> item.getScore() > 0d || shouldKeepScopedItem(item, request)) // 过滤低分但需保留的项
      .sorted(Comparator.comparingDouble(RelatedKnowledgeVO::getScore).reversed()) // 按得分降序
      .limit(resolveLimit(request))                        // 截取指定数量
      .toList();
  }

  /**
   * 根据意图类型过滤知识来源
   *
   * 规则：
   * - 政策问答：只保留 RULE 类型
   * - 实时状态：保留当前商品或 RULE 类型
   * - 对比推荐：只保留商品类型（PRODUCT/SECKILL），且必须是当前商品或对比候选商品
   * - 产品事实：只保留商品类型，若指定了当前商品则只保留该商品的知识
   *
   * @param chunk   知识分块
   * @param request 检索请求
   * @return 是否保留该分块
   */
  private boolean matchesIntentScope(KnowledgeChunkPO chunk, KnowledgeRetrieveRequest request) {
    QuestionIntentType intentType = request.getIntentType();
    Long currentProductId = request.getCurrentProductId();
    Set<String> compareIds = request.getCompareCandidateIds().stream()
      .map(String::valueOf)
      .collect(Collectors.toSet());

    if (intentType == QuestionIntentType.POLICY_QA) {
      return chunk.getSourceType() == KnowledgeSourceType.RULE;
    }
    if (intentType == QuestionIntentType.REALTIME_STATUS) {
      return currentProductId == null
        || Objects.equals(String.valueOf(currentProductId), chunk.getSourceId())
        || chunk.getSourceType() == KnowledgeSourceType.RULE;
    }
    if (intentType == QuestionIntentType.COMPARE_RECOMMENDATION) {
      if (!isProductSource(chunk)) {
        return false;
      }
      if (currentProductId != null && Objects.equals(String.valueOf(currentProductId), chunk.getSourceId())) {
        return true;
      }
      return compareIds.contains(chunk.getSourceId());
    }
    if (intentType == QuestionIntentType.PRODUCT_FACT) {
      if (!isProductSource(chunk)) {
        return false;
      }
      return currentProductId == null || Objects.equals(String.valueOf(currentProductId), chunk.getSourceId());
    }
    return false;
  }

  /**
   * 根据问题类别进一步过滤
   *
   * - 售后/配送政策：只保留 RULE 类型
   * - 活动规则：排除纯 PRODUCT 类型（因为活动信息可能来自规则或秒杀描述）
   *
   * @param chunk   知识分块
   * @param request 检索请求
   * @return 是否保留
   */
  private boolean matchesCategory(KnowledgeChunkPO chunk, KnowledgeRetrieveRequest request) {
    QuestionCategory category = request.getCategory();
    if (category == QuestionCategory.AFTER_SALES_POLICY || category == QuestionCategory.DELIVERY_POLICY) {
      return chunk.getSourceType() == KnowledgeSourceType.RULE;
    }
    if (category == QuestionCategory.ACTIVITY_RULE) {
      return chunk.getSourceType() != KnowledgeSourceType.PRODUCT;
    }
    return true;
  }

  /**
   * 将分块 PO 转换为结果 VO，并计算得分
   */
  private RelatedKnowledgeVO toKnowledge(KnowledgeChunkPO chunk, KnowledgeRetrieveRequest request) {
    RelatedKnowledgeVO vo = new RelatedKnowledgeVO();
    vo.setDocumentId(chunk.getDocumentId());
    vo.setTitle(chunk.getTitle());
    vo.setSourceType(chunk.getSourceType().name());
    vo.setSourceId(chunk.getSourceId());
    vo.setSnippet(chunk.getContent());
    vo.setScore(calculateScore(chunk, request));
    vo.setRealtime(false);
    return vo;
  }

  /**
   * 计算知识分块的综合得分
   *
   * 得分构成：
   * 1. 向量相似度（余弦相似度）
   * 2. 词汇增强（标题/内容匹配问题文本）
   * 3. 上下文加权（当前商品、对比候选商品额外加分）
   *
   * 最终得分上限为 1.0
   *
   * @param chunk   知识分块
   * @param request 检索请求
   * @return 综合得分
   */
  private double calculateScore(KnowledgeChunkPO chunk, KnowledgeRetrieveRequest request) {
    double vectorScore = SimilarityUtils.cosineSimilarity(request.getQuestionEmbedding(), chunk.getEmbedding());
    double score = vectorScore + lexicalBoost(request.getRewrittenQuestion(), chunk);

    // 当前锁定商品加分
    if (request.getCurrentProductId() != null
      && Objects.equals(String.valueOf(request.getCurrentProductId()), chunk.getSourceId())
      && isProductSource(chunk)) {
      score += CURRENT_PRODUCT_BOOST;
    }
    // 对比候选商品加分
    if (request.getCompareCandidateIds().contains(parseLong(chunk.getSourceId())) && isProductSource(chunk)) {
      score += COMPARE_CANDIDATE_BOOST;
    }
    return Math.min(1d, score);
  }

  /**
   * 判断是否应强制保留该知识片段（即使得分很低）
   *
   * 适用于产品事实和对比推荐场景：当前锁定商品或对比候选商品的知识必须保留，
   * 以确保大模型至少有基础的商品信息可以回答。
   *
   * @param item    知识片段
   * @param request 检索请求
   * @return 是否强制保留
   */
  private boolean shouldKeepScopedItem(RelatedKnowledgeVO item, KnowledgeRetrieveRequest request) {
    if (request.getIntentType() == QuestionIntentType.PRODUCT_FACT
      || request.getIntentType() == QuestionIntentType.COMPARE_RECOMMENDATION) {
      if (request.getCurrentProductId() != null
        && Objects.equals(String.valueOf(request.getCurrentProductId()), item.getSourceId())
        && isProductSource(item)) {
        return true;
      }
      return request.getCompareCandidateIds().stream()
        .map(String::valueOf)
        .anyMatch(candidateId -> candidateId.equals(item.getSourceId()) && isProductSource(item));
    }
    return false;
  }

  /**
   * 根据意图类型和类别动态决定返回的知识片段数量上限
   *
   * 避免 Prompt 过长，同时保证关键场景有足够证据：
   * - 对比推荐：至少5条，便于多维度比较
   * - 产品信息：3条
   * - 政策问答：2条
   * - 实时状态/活动规则：4条
   *
   * @param request 检索请求
   * @return 数量上限
   */
  private int resolveLimit(KnowledgeRetrieveRequest request) {
    if (request.getIntentType() == QuestionIntentType.COMPARE_RECOMMENDATION) {
      return Math.max(5, aiProperties.getRetrievalTopK());
    }
    return switch (request.getCategory()) {
      case PRODUCT_INFO -> 3;
      case COMPARE_RECOMMENDATION -> 6;
      case AFTER_SALES_POLICY, DELIVERY_POLICY -> 2;
      case ACTIVITY_RULE, REALTIME_STATUS -> 4;
      default -> aiProperties.getRetrievalTopK();
    };
  }

  /**
   * 词汇匹配增强得分
   *
   * 检查问题文本是否与标题或内容存在字面匹配，给予额外加分。
   * 加分规则：
   * - 问题完全包含标题：EXACT_TITLE_BOOST (0.65)
   * - 标题与问题有分词重叠（且非规则文档）：PARTIAL_TITLE_BOOST (0.25)
   * - 内容包含问题：CONTENT_BOOST (0.10)
   *
   * @param question 用户问题（改写后）
   * @param chunk    知识分块
   * @return 额外加分值
   */
  private double lexicalBoost(String question, KnowledgeChunkPO chunk) {
    String normalizedQuestion = normalize(question);
    if (!StringUtils.hasText(normalizedQuestion)) {
      return 0d;
    }

    String normalizedTitle = normalize(chunk.getTitle());
    if (StringUtils.hasText(normalizedTitle)) {
      if (normalizedQuestion.contains(normalizedTitle)) {
        return EXACT_TITLE_BOOST;
      }
      // 规则文档的标题通常较短且通用，避免误加分
      if (chunk.getSourceType() != KnowledgeSourceType.RULE && overlapByTokens(normalizedQuestion, normalizedTitle)) {
        return PARTIAL_TITLE_BOOST;
      }
    }

    String normalizedContent = normalize(chunk.getContent());
    if (StringUtils.hasText(normalizedContent) && normalizedContent.contains(normalizedQuestion)) {
      return CONTENT_BOOST;
    }
    return 0d;
  }

  /**
   * 判断两个文本是否存在分词重叠（至少一个长度≥2的 token 在目标文本中出现）
   */
  private boolean overlapByTokens(String normalizedQuestion, String normalizedTitle) {
    for (String token : normalizedTitle.split("\\s+")) {
      if (token.length() >= 2 && normalizedQuestion.contains(token)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 判断分块是否为商品来源（普通商品或秒杀商品）
   */
  private boolean isProductSource(KnowledgeChunkPO chunk) {
    return chunk.getSourceType() == KnowledgeSourceType.PRODUCT
      || chunk.getSourceType() == KnowledgeSourceType.SECKILL;
  }

  /**
   * 判断结果 VO 是否为商品来源
   */
  private boolean isProductSource(RelatedKnowledgeVO knowledge) {
    return "PRODUCT".equalsIgnoreCase(knowledge.getSourceType())
      || "SECKILL".equalsIgnoreCase(knowledge.getSourceType());
  }

  /**
   * 将字符串安全转换为 Long，失败返回 null
   */
  private Long parseLong(String sourceId) {
    try {
      return sourceId == null ? null : Long.parseLong(sourceId);
    } catch (NumberFormatException ignore) {
      return null;
    }
  }

  /**
   * 文本规范化：转小写、去除非字母数字中文的字符、合并空格
   */
  private String normalize(String text) {
    if (!StringUtils.hasText(text)) {
      return "";
    }
    return text.toLowerCase(Locale.ROOT)
      .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\u4e00-\\u9fa5]+", " ")
      .trim()
      .replaceAll("\\s+", " ");
  }
}
