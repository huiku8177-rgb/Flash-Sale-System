package com.flashsale.aiservice.service.impl;

import com.flashsale.aiservice.domain.enums.AnswerPolicy;
import com.flashsale.aiservice.domain.vo.RelatedKnowledgeVO;
import com.flashsale.aiservice.service.ChatAuditService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatAuditServiceImpl implements ChatAuditService {

    @Override
    public String buildAuditSummary(String question, String answer, AnswerPolicy answerPolicy, String fallbackReason,
                                    List<RelatedKnowledgeVO> hitKnowledge) {
        String sourceSummary = hitKnowledge == null || hitKnowledge.isEmpty()
                ? "no knowledge hit"
                : hitKnowledge.stream()
                        .map(item -> item.getTitle() + ":" + String.format("%.2f", item.getScore()))
                        .collect(Collectors.joining(", "));

        String answerPreview = answer == null ? "" : answer.substring(0, Math.min(answer.length(), 120));
        return "policy=" + answerPolicy.name()
                + ", fallbackReason=" + (fallbackReason == null ? "" : fallbackReason)
                + ", question=" + trim(question, 120)
                + ", answerPreview=" + answerPreview
                + ", sources=" + sourceSummary;
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
