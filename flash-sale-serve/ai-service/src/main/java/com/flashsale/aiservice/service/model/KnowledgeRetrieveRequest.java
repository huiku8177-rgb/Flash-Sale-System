package com.flashsale.aiservice.service.model;

import com.flashsale.aiservice.domain.enums.QuestionCategory;
import com.flashsale.aiservice.domain.enums.QuestionIntentType;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class KnowledgeRetrieveRequest {

    private String question;
    private String rewrittenQuestion;
    private List<Double> questionEmbedding = new ArrayList<>();
    private QuestionCategory category;
    private QuestionIntentType intentType;
    private Long currentProductId;
    private String currentProductName;
    private String currentProductType;
    private List<Long> compareCandidateIds = new ArrayList<>();
    private List<String> compareCandidateKeys = new ArrayList<>();
}
