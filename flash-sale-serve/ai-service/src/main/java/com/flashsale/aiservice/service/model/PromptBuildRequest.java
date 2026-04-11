package com.flashsale.aiservice.service.model;

import com.flashsale.aiservice.domain.enums.QuestionCategory;
import com.flashsale.aiservice.domain.enums.QuestionIntentType;
import com.flashsale.aiservice.domain.po.ChatRecordPO;
import com.flashsale.aiservice.domain.vo.ConversationContextState;
import com.flashsale.aiservice.domain.vo.ProductCandidateVO;
import com.flashsale.aiservice.domain.vo.RelatedKnowledgeVO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PromptBuildRequest {

    private String question;
    private String rewrittenQuestion;
    private QuestionCategory category;
    private QuestionIntentType intentType;
    private String routeType;
    private List<RelatedKnowledgeVO> knowledgeList = new ArrayList<>();
    private List<ChatRecordPO> history = new ArrayList<>();
    private String realtimeFacts;
    private ConversationContextState contextState;
    private List<ProductCandidateVO> compareCandidates = new ArrayList<>();
}
