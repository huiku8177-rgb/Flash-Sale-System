package com.flashsale.aiservice.service.impl;

import com.flashsale.aiservice.config.AiProperties;
import com.flashsale.aiservice.domain.enums.KnowledgeSourceType;
import com.flashsale.aiservice.domain.enums.QuestionCategory;
import com.flashsale.aiservice.domain.po.KnowledgeChunkPO;
import com.flashsale.aiservice.domain.vo.RelatedKnowledgeVO;
import com.flashsale.aiservice.service.KnowledgeRetrievalService;
import com.flashsale.aiservice.service.VectorStoreService;
import com.flashsale.aiservice.util.SimilarityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalServiceImpl implements KnowledgeRetrievalService {

    private static final double EXACT_TITLE_BOOST = 0.65d;
    private static final double PARTIAL_TITLE_BOOST = 0.25d;
    private static final double CONTENT_BOOST = 0.10d;

    private final VectorStoreService vectorStoreService;
    private final AiProperties aiProperties;

    @Override
    public List<RelatedKnowledgeVO> retrieve(String question, List<Double> questionEmbedding, QuestionCategory category, Long productId) {
        Stream<KnowledgeChunkPO> stream = vectorStoreService.allChunks().stream()
                .filter(chunk -> matchesProductScope(chunk, productId))
                .filter(chunk -> matchesCategory(chunk, category, question));

        return stream
                .map(chunk -> toKnowledge(question, questionEmbedding, chunk))
                .filter(item -> item.getScore() > 0d)
                .sorted(Comparator.comparingDouble(RelatedKnowledgeVO::getScore).reversed())
                .limit(aiProperties.getRetrievalTopK())
                .toList();
    }

    private boolean matchesProductScope(KnowledgeChunkPO chunk, Long productId) {
        if (productId == null) {
            return true;
        }
        if (Objects.equals(String.valueOf(productId), chunk.getSourceId())) {
            return true;
        }
        return chunk.getSourceType() == KnowledgeSourceType.RULE;
    }

    private boolean matchesCategory(KnowledgeChunkPO chunk, QuestionCategory category, String question) {
        if (category == QuestionCategory.OUT_OF_SCOPE) {
            return false;
        }
        if (category == QuestionCategory.AFTER_SALES_POLICY || category == QuestionCategory.DELIVERY_POLICY) {
            return chunk.getSourceType() == KnowledgeSourceType.RULE;
        }
        if (category == QuestionCategory.ACTIVITY_RULE) {
            return chunk.getSourceType() != KnowledgeSourceType.PRODUCT;
        }
        if (category == QuestionCategory.REALTIME_STATUS && question != null) {
            String lower = question.toLowerCase(Locale.ROOT);
            if (lower.contains("\u79d2\u6740") || lower.contains("\u6d3b\u52a8")) {
                return chunk.getSourceType() != KnowledgeSourceType.PRODUCT;
            }
        }
        return true;
    }

    private RelatedKnowledgeVO toKnowledge(String question, List<Double> questionEmbedding, KnowledgeChunkPO chunk) {
        RelatedKnowledgeVO vo = new RelatedKnowledgeVO();
        vo.setDocumentId(chunk.getDocumentId());
        vo.setTitle(chunk.getTitle());
        vo.setSourceType(chunk.getSourceType().name());
        vo.setSourceId(chunk.getSourceId());
        vo.setSnippet(chunk.getContent());
        vo.setScore(calculateScore(question, questionEmbedding, chunk));
        vo.setRealtime(false);
        return vo;
    }

    private double calculateScore(String question, List<Double> questionEmbedding, KnowledgeChunkPO chunk) {
        double vectorScore = SimilarityUtils.cosineSimilarity(questionEmbedding, chunk.getEmbedding());
        return Math.min(1d, vectorScore + lexicalBoost(question, chunk));
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
