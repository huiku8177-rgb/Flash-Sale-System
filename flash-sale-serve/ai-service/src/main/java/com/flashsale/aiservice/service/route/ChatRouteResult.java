package com.flashsale.aiservice.service.route;

import com.flashsale.aiservice.domain.enums.AnswerPolicy;
import com.flashsale.aiservice.domain.enums.QuestionCategory;
import com.flashsale.aiservice.domain.enums.QuestionIntentType;
import com.flashsale.aiservice.domain.vo.ConversationContextState;
import com.flashsale.aiservice.domain.vo.ProductCandidateVO;
import com.flashsale.aiservice.domain.vo.RelatedKnowledgeVO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChatRouteResult {

    private String answer;
    private List<String> sources = new ArrayList<>();
    private List<RelatedKnowledgeVO> hitKnowledge = new ArrayList<>();
    private double confidence;
    private String fallbackReason;
    private AnswerPolicy answerPolicy;
    private QuestionCategory category;
    private QuestionIntentType intentType;
    private String routeType;
    private String rewrittenQuestion;
    private ConversationContextState contextState;
    private List<ProductCandidateVO> compareCandidates = new ArrayList<>();
}
