package com.flashsale.aiservice.service;

import com.flashsale.aiservice.domain.enums.QuestionCategory;
import com.flashsale.aiservice.domain.po.ChatRecordPO;
import com.flashsale.aiservice.domain.vo.RelatedKnowledgeVO;

import java.util.List;

public interface PromptBuilderService {

    String buildPrompt(String question, QuestionCategory category, List<RelatedKnowledgeVO> knowledgeList,
                       List<ChatRecordPO> history, String realtimeFacts);
}
