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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalServiceImpl implements KnowledgeRetrievalService {

  private static final double EXACT_TITLE_BOOST = 0.65d;
  private static final double PARTIAL_TITLE_BOOST = 0.25d;
  private static final double CONTENT_BOOST = 0.10d;
  private static final double CURRENT_PRODUCT_BOOST = 0.35d;
  private static final double COMPARE_CANDIDATE_BOOST = 0.18d;

  private final VectorStoreService vectorStoreService;
  private final AiProperties aiProperties;

  @Override
  public List<RelatedKnowledgeVO> retrieve(KnowledgeRetrieveRequest request) {
    if (request == null || request.getCategory() == QuestionCategory.OUT_OF_SCOPE) {
      return List.of();
    }

    Stream<KnowledgeChunkPO> stream = vectorStoreService.allChunks().stream()
      .filter(chunk -> matchesIntentScope(chunk, request))
      .filter(chunk -> matchesCategory(chunk, request));

    return stream
      .map(chunk -> toKnowledge(chunk, request))
      .filter(item -> item.getScore() > 0d || shouldKeepScopedItem(item, request))
      .sorted(Comparator.comparingDouble(RelatedKnowledgeVO::getScore).reversed())
      .limit(resolveLimit(request))
      .toList();
  }

  private boolean matchesIntentScope(KnowledgeChunkPO chunk, KnowledgeRetrieveRequest request) {
    QuestionIntentType intentType = request.getIntentType();
    String currentProductKey = productKey(request.getCurrentProductType(), request.getCurrentProductId());
    Set<String> compareKeys = request.getCompareCandidateKeys().stream()
      .filter(StringUtils::hasText)
      .collect(Collectors.toSet());

    if (intentType == QuestionIntentType.POLICY_QA) {
      return chunk.getSourceType() == KnowledgeSourceType.RULE;
    }
    if (intentType == QuestionIntentType.REALTIME_STATUS) {
      return matchesCurrentProduct(chunk, request, currentProductKey)
        || chunk.getSourceType() == KnowledgeSourceType.RULE;
    }
    if (intentType == QuestionIntentType.COMPARE_RECOMMENDATION) {
      if (!isProductSource(chunk)) {
        return false;
      }
      if (matchesCurrentProduct(chunk, request, currentProductKey)) {
        return true;
      }
      return compareKeys.contains(productKey(chunk));
    }
    if (intentType == QuestionIntentType.PRODUCT_FACT) {
      if (!isProductSource(chunk)) {
        return false;
      }
      return request.getCurrentProductId() == null || matchesCurrentProduct(chunk, request, currentProductKey);
    }
    return false;
  }

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

  private double calculateScore(KnowledgeChunkPO chunk, KnowledgeRetrieveRequest request) {
    double vectorScore = SimilarityUtils.cosineSimilarity(request.getQuestionEmbedding(), chunk.getEmbedding());
    double score = vectorScore + lexicalBoost(request.getRewrittenQuestion(), chunk);

    if (matchesCurrentProduct(chunk, request, null) && isProductSource(chunk)) {
      score += CURRENT_PRODUCT_BOOST;
    }
    if (request.getCompareCandidateKeys().contains(productKey(chunk)) && isProductSource(chunk)) {
      score += COMPARE_CANDIDATE_BOOST;
    }
    return Math.min(1d, score);
  }

  private boolean shouldKeepScopedItem(RelatedKnowledgeVO item, KnowledgeRetrieveRequest request) {
    if (request.getIntentType() == QuestionIntentType.PRODUCT_FACT
      || request.getIntentType() == QuestionIntentType.COMPARE_RECOMMENDATION) {
      if (matchesCurrentProduct(item, request) && isProductSource(item)) {
        return true;
      }
      return request.getCompareCandidateKeys().contains(productKey(item)) && isProductSource(item);
    }
    return false;
  }

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

  private boolean overlapByTokens(String normalizedQuestion, String normalizedTitle) {
    for (String token : normalizedTitle.split("\\s+")) {
      if (token.length() >= 2 && normalizedQuestion.contains(token)) {
        return true;
      }
    }
    return false;
  }

  private boolean isProductSource(KnowledgeChunkPO chunk) {
    return chunk.getSourceType() == KnowledgeSourceType.PRODUCT
      || chunk.getSourceType() == KnowledgeSourceType.SECKILL;
  }

  private boolean isProductSource(RelatedKnowledgeVO knowledge) {
    return "PRODUCT".equalsIgnoreCase(knowledge.getSourceType())
      || "SECKILL".equalsIgnoreCase(knowledge.getSourceType());
  }

  private boolean matchesCurrentProduct(KnowledgeChunkPO chunk, KnowledgeRetrieveRequest request, String currentProductKey) {
    if (request.getCurrentProductId() == null || !isProductSource(chunk)) {
      return false;
    }
    String expectedKey = StringUtils.hasText(currentProductKey)
      ? currentProductKey
      : productKey(request.getCurrentProductType(), request.getCurrentProductId());
    if (StringUtils.hasText(expectedKey)) {
      return expectedKey.equals(productKey(chunk));
    }
    return String.valueOf(request.getCurrentProductId()).equals(chunk.getSourceId());
  }

  private boolean matchesCurrentProduct(RelatedKnowledgeVO knowledge, KnowledgeRetrieveRequest request) {
    if (request.getCurrentProductId() == null || !isProductSource(knowledge)) {
      return false;
    }
    String expectedKey = productKey(request.getCurrentProductType(), request.getCurrentProductId());
    if (StringUtils.hasText(expectedKey)) {
      return expectedKey.equals(productKey(knowledge));
    }
    return String.valueOf(request.getCurrentProductId()).equals(knowledge.getSourceId());
  }

  private String productKey(KnowledgeChunkPO chunk) {
    return chunk == null ? "" : productKey(chunk.getSourceType(), parseLong(chunk.getSourceId()));
  }

  private String productKey(RelatedKnowledgeVO knowledge) {
    return knowledge == null ? "" : productKey(parseSourceType(knowledge.getSourceType()), parseLong(knowledge.getSourceId()));
  }

  private String productKey(String productType, Long productId) {
    return productKey(parseProductType(productType), productId);
  }

  private String productKey(KnowledgeSourceType sourceType, Long productId) {
    return sourceType == null || productId == null ? "" : sourceType.name() + ":" + productId;
  }

  private KnowledgeSourceType parseProductType(String productType) {
    if (!StringUtils.hasText(productType)) {
      return null;
    }
    if ("normal".equalsIgnoreCase(productType)) {
      return KnowledgeSourceType.PRODUCT;
    }
    if ("seckill".equalsIgnoreCase(productType)) {
      return KnowledgeSourceType.SECKILL;
    }
    return parseSourceType(productType);
  }

  private KnowledgeSourceType parseSourceType(String sourceType) {
    if (!StringUtils.hasText(sourceType)) {
      return null;
    }
    try {
      return KnowledgeSourceType.valueOf(sourceType.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ignore) {
      return null;
    }
  }

  private Long parseLong(String sourceId) {
    try {
      return sourceId == null ? null : Long.parseLong(sourceId);
    } catch (NumberFormatException ignore) {
      return null;
    }
  }

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
