package com.flashsale.aiservice.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChatRecordVO {

    private Integer recordNo;
    private String question;
    private String questionCategory;
    private String intentType;
    private String routeType;
    private String rewrittenQuestion;
    private String answer;
    private String answerPolicy;
    private List<String> sources;
    private List<RelatedKnowledgeVO> hitKnowledge;
    private List<ProductCandidateVO> compareCandidates;
    private BigDecimal confidence;
    private String fallbackReason;
    private String auditSummary;
    private LocalDateTime createdAt;
}
