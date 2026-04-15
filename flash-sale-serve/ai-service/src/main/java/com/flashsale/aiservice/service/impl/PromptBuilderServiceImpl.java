package com.flashsale.aiservice.service.impl;

import com.flashsale.aiservice.domain.po.ChatRecordPO;
import com.flashsale.aiservice.domain.vo.ProductCandidateVO;
import com.flashsale.aiservice.domain.vo.RelatedKnowledgeVO;
import com.flashsale.aiservice.service.PromptBuilderService;
import com.flashsale.aiservice.service.model.PromptBuildRequest;
import com.flashsale.aiservice.service.model.PromptMessageBundle;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromptBuilderServiceImpl implements PromptBuilderService {

  @Override
  public PromptMessageBundle buildPromptBundle(PromptBuildRequest request) {
    StringBuilder systemPrompt = new StringBuilder();
    StringBuilder userPrompt = new StringBuilder();

    systemPrompt.append("You are an e-commerce product assistant. Answer in concise Chinese.\n");
    systemPrompt.append("Intent type: ").append(request.getIntentType()).append("\n");
    systemPrompt.append("Route type: ").append(request.getRouteType()).append("\n");
    systemPrompt.append("Category: ").append(request.getCategory()).append("\n\n");
    appendGlobalRules(systemPrompt, request);
    systemPrompt.append("[Answer requirements]\n");
    switch (request.getIntentType()) {
      case PRODUCT_FACT -> appendProductFactRequirements(systemPrompt);
      case REALTIME_STATUS -> appendRealtimeRequirements(systemPrompt);
      case POLICY_QA -> appendPolicyRequirements(systemPrompt);
      case COMPARE_RECOMMENDATION -> appendCompareRequirements(systemPrompt);
      default -> systemPrompt.append("- Directly answer the user in natural Chinese.\n");
    }

    appendContext(userPrompt, request);
    appendRealtimeFacts(userPrompt, request);
    appendKnowledge(userPrompt, request);
    appendCompareCandidates(userPrompt, request);
    appendHistory(userPrompt, request);
    userPrompt.append("[Original user question]\n").append(request.getQuestion()).append("\n\n");
    userPrompt.append("[Rewritten user question]\n")
      .append(StringUtils.hasText(request.getRewrittenQuestion()) ? request.getRewrittenQuestion() : request.getQuestion())
      .append("\n");
    return new PromptMessageBundle(systemPrompt.toString(), userPrompt.toString());
  }

  private void appendGlobalRules(StringBuilder prompt, PromptBuildRequest request) {
    prompt.append("[Global rules]\n");
    prompt.append("- Only use the supplied evidence and realtime facts.\n");
    prompt.append("- Do not fabricate parameters, policies, prices, stock, time or competitor facts.\n");
    prompt.append("- Treat recent conversation as background only, never as authoritative evidence.\n");
    prompt.append("- If you make a reasonable inference from the current product description, explicitly say it is an inference.\n");
    prompt.append("- If evidence is insufficient, explicitly say what is missing instead of pretending to know.\n");
    if (request.getIntentType() == com.flashsale.aiservice.domain.enums.QuestionIntentType.PRODUCT_FACT) {
      prompt.append("- Do not mention other products or policy rules unless they are directly relevant.\n");
    }
    if (request.getIntentType() == com.flashsale.aiservice.domain.enums.QuestionIntentType.COMPARE_RECOMMENDATION) {
      prompt.append("- Comparison must be based only on the listed comparison candidates.\n");
    }
    prompt.append("\n");
  }

  private void appendContext(StringBuilder prompt, PromptBuildRequest request) {
    prompt.append("[Conversation context]\n");
    if (request.getContextState() != null) {
      if (request.getContextState().getCurrentProductId() != null) {
        prompt.append("- Current product id: ").append(request.getContextState().getCurrentProductId()).append("\n");
      }
      if (StringUtils.hasText(request.getContextState().getCurrentProductName())) {
        prompt.append("- Current product name: ").append(request.getContextState().getCurrentProductName()).append("\n");
      }
      if (StringUtils.hasText(request.getContextState().getCurrentIntentType())) {
        prompt.append("- Previous intent type: ").append(request.getContextState().getCurrentIntentType()).append("\n");
      }
    }
    prompt.append("\n");
  }

  private void appendRealtimeFacts(StringBuilder prompt, PromptBuildRequest request) {
    if (!StringUtils.hasText(request.getRealtimeFacts())) {
      return;
    }
    prompt.append("[Realtime facts]\n").append(request.getRealtimeFacts()).append("\n\n");
  }

  private void appendKnowledge(StringBuilder prompt, PromptBuildRequest request) {
    if (request.getKnowledgeList() == null || request.getKnowledgeList().isEmpty()) {
      return;
    }
    prompt.append("[Evidence]\n");
    for (RelatedKnowledgeVO knowledge : request.getKnowledgeList()) {
      prompt.append("- SourceType: ").append(knowledge.getSourceType())
        .append("; Title: ").append(knowledge.getTitle())
        .append("; SourceId: ").append(knowledge.getSourceId())
        .append("; Content: ").append(knowledge.getSnippet())
        .append("\n");
    }
    prompt.append("\n");
  }

  private void appendCompareCandidates(StringBuilder prompt, PromptBuildRequest request) {
    if (request.getCompareCandidates() == null || request.getCompareCandidates().isEmpty()) {
      return;
    }
    prompt.append("[Comparison candidates]\n");
    for (ProductCandidateVO candidate : request.getCompareCandidates()) {
      prompt.append("- ")
        .append(candidate.getName())
        .append(" (type=").append(candidate.getProductType())
        .append(", price=").append(candidate.getPrice())
        .append(", score=").append(candidate.getScore())
        .append(")\n");
    }
    prompt.append("\n");
  }

  private void appendHistory(StringBuilder prompt, PromptBuildRequest request) {
    if (request.getHistory() == null || request.getHistory().isEmpty()) {
      return;
    }
    prompt.append("[Recent user questions for background only]\n");
    for (ChatRecordPO record : request.getHistory()) {
      if (StringUtils.hasText(record.getQuestion())) {
        prompt.append("User: ").append(record.getQuestion()).append("\n");
      }
    }
    prompt.append("\n");
  }

  private void appendProductFactRequirements(StringBuilder prompt) {
    prompt.append("- Focus on the current product only.\n");
    prompt.append("- You may summarize selling points, use cases, target users and value proposition.\n");
    prompt.append("- If the question asks who this product suits, infer cautiously from title, subtitle, detail and features.\n");
    prompt.append("- Keep the answer concrete and directly relevant to the user question.\n");
  }

  private void appendRealtimeRequirements(StringBuilder prompt) {
    prompt.append("- Answer with realtime facts first.\n");
    prompt.append("- For price, stock or activity status, use only the supplied realtime facts.\n");
    prompt.append("- If the current product is not explicit enough, ask the user to specify the product instead of guessing.\n");
    prompt.append("- Do not add speculative recommendations unless clearly supported.\n");
  }

  private void appendPolicyRequirements(StringBuilder prompt) {
    prompt.append("- Answer strictly from the supplied policy evidence.\n");
    prompt.append("- Keep the answer explicit and practical.\n");
    prompt.append("- Do not mix in unrelated product descriptions.\n");
  }

  private void appendCompareRequirements(StringBuilder prompt) {
    prompt.append("- Produce four short parts in Chinese:\n");
    prompt.append("  1. Current product strengths\n");
    prompt.append("  2. Current product limitations\n");
    prompt.append("  3. Suitable users or scenarios\n");
    prompt.append("  4. Buy recommendation and reason\n");
    prompt.append("- Comparison must only rely on the listed candidates and current product evidence.\n");
    prompt.append("- If comparison evidence is limited, say the recommendation is based on limited current evidence.\n");
    prompt.append("- Avoid generic brand praise or unsupported claims.\n");
  }
}
