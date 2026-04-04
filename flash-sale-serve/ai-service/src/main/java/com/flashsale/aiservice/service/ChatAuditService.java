package com.flashsale.aiservice.service;

import com.flashsale.aiservice.domain.enums.AnswerPolicy;
import com.flashsale.aiservice.domain.vo.RelatedKnowledgeVO;

import java.util.List;

public interface ChatAuditService {

    String buildAuditSummary(String question, String answer, AnswerPolicy answerPolicy, String fallbackReason,
                             List<RelatedKnowledgeVO> hitKnowledge);
}
