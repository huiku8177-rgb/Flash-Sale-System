package com.flashsale.aiservice.service;

import com.flashsale.aiservice.domain.enums.QuestionCategory;
import com.flashsale.aiservice.domain.vo.RelatedKnowledgeVO;

import java.util.List;

public interface KnowledgeRetrievalService {

    List<RelatedKnowledgeVO> retrieve(String question, List<Double> questionEmbedding, QuestionCategory category, Long productId);
}
