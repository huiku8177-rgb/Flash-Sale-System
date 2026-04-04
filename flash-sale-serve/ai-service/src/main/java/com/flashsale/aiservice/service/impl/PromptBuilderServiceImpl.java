package com.flashsale.aiservice.service.impl;

import com.flashsale.aiservice.domain.enums.QuestionCategory;
import com.flashsale.aiservice.domain.po.ChatRecordPO;
import com.flashsale.aiservice.domain.vo.RelatedKnowledgeVO;
import com.flashsale.aiservice.service.PromptBuilderService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PromptBuilderServiceImpl implements PromptBuilderService {

    @Override
    public String buildPrompt(String question, QuestionCategory category, List<RelatedKnowledgeVO> knowledgeList,
                              List<ChatRecordPO> history, String realtimeFacts) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a product customer-service assistant for an e-commerce system.\n");
        prompt.append("Answer only from the provided business facts and retrieved knowledge.\n");
        prompt.append("If the evidence is insufficient, explicitly say you cannot confirm and advise the user to rely on the page or human customer service.\n");
        prompt.append("Do not invent product details, activity rules, prices, stock, or delivery promises.\n");
        prompt.append("Question category: ").append(category.name()).append("\n\n");

        if (realtimeFacts != null && !realtimeFacts.isBlank()) {
            prompt.append("[Realtime Facts]\n").append(realtimeFacts).append("\n\n");
        }

        if (knowledgeList != null && !knowledgeList.isEmpty()) {
            prompt.append("[Knowledge Evidence]\n");
            for (RelatedKnowledgeVO knowledge : knowledgeList) {
                prompt.append("- [")
                        .append(knowledge.getSourceType())
                        .append("] ")
                        .append(knowledge.getTitle())
                        .append(": ")
                        .append(knowledge.getSnippet())
                        .append("\n");
            }
            prompt.append("\n");
        }

        if (history != null && !history.isEmpty()) {
            prompt.append("[Recent Conversation]\n");
            for (ChatRecordPO record : history) {
                prompt.append("User: ").append(record.getQuestion()).append("\n");
                prompt.append("Assistant: ").append(record.getAnswer()).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("[User Question]\n").append(question);
        return prompt.toString();
    }
}
