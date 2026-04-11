package com.flashsale.aiservice.service.impl;

import com.flashsale.aiservice.domain.po.ChatRecordPO;
import com.flashsale.aiservice.domain.vo.ProductCandidateVO;
import com.flashsale.aiservice.domain.vo.RelatedKnowledgeVO;
import com.flashsale.aiservice.service.PromptBuilderService;
import com.flashsale.aiservice.service.model.PromptBuildRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Prompt 构建服务实现类
 *
 * 负责根据不同的意图类型、路由类型和上下文信息，拼接出结构化的提示词（Prompt），
 * 用于调用大模型生成回答。构建的 Prompt 包含以下模块：
 * - 全局规则（引用证据限制、禁止捏造）
 * - 会话上下文（当前商品ID、名称等）
 * - 实时数据（价格、库存）
 * - 知识证据（向量检索结果）
 * - 对比候选商品（对比场景专用）
 * - 历史对话
 * - 回答要求（根据意图类型定制）
 */
@Service
public class PromptBuilderServiceImpl implements PromptBuilderService {

  /**
   * 构建完整的 Prompt 字符串
   *
   * @param request 包含所有构建所需信息的请求对象
   * @return 格式化后的 Prompt
   */
  @Override
  public String buildPrompt(PromptBuildRequest request) {
    StringBuilder prompt = new StringBuilder();

    // 角色设定与基本指令
    prompt.append("You are an e-commerce product assistant. Answer in concise Chinese.\n");
    prompt.append("Intent type: ").append(request.getIntentType()).append("\n");
    prompt.append("Route type: ").append(request.getRouteType()).append("\n");
    prompt.append("Category: ").append(request.getCategory()).append("\n\n");

    // 按顺序拼接各个模块
    appendGlobalRules(prompt, request);          // 全局规则
    appendContext(prompt, request);               // 上下文信息
    appendRealtimeFacts(prompt, request);         // 实时数据
    appendKnowledge(prompt, request);             // 知识证据
    appendCompareCandidates(prompt, request);     // 对比候选商品
    appendHistory(prompt, request);               // 历史对话

    // 用户问题部分
    prompt.append("[Original user question]\n").append(request.getQuestion()).append("\n\n");
    prompt.append("[Rewritten user question]\n")
      .append(StringUtils.hasText(request.getRewrittenQuestion()) ? request.getRewrittenQuestion() : request.getQuestion())
      .append("\n\n");

    // 根据意图类型添加定制化的回答要求
    prompt.append("[Answer requirements]\n");
    switch (request.getIntentType()) {
      case PRODUCT_FACT -> appendProductFactRequirements(prompt);
      case REALTIME_STATUS -> appendRealtimeRequirements(prompt);
      case POLICY_QA -> appendPolicyRequirements(prompt);
      case COMPARE_RECOMMENDATION -> appendCompareRequirements(prompt);
      default -> prompt.append("- Directly answer the user in natural Chinese.\n");
    }
    return prompt.toString();
  }

  /**
   * 添加全局行为约束规则
   *
   * 要求模型：
   * - 仅使用提供的证据和实时数据
   * - 不得捏造参数、政策、价格、库存等信息
   * - 合理推断需注明来源
   * - 证据不足时坦诚说明
   */
  private void appendGlobalRules(StringBuilder prompt, PromptBuildRequest request) {
    prompt.append("[Global rules]\n");
    prompt.append("- Only use the supplied evidence and realtime facts.\n");
    prompt.append("- Do not fabricate parameters, policies, prices, stock, time or competitor facts.\n");
    prompt.append("- If you make a reasonable inference from the current product description, clearly say ")
      .append("\"根据当前商品描述来看\" or \"从当前卖点来看\".\n");
    prompt.append("- If evidence is insufficient, explicitly say what is missing instead of pretending to know.\n");
    if (request.getIntentType() == com.flashsale.aiservice.domain.enums.QuestionIntentType.PRODUCT_FACT) {
      prompt.append("- Do not mention other products or policy rules unless they are directly relevant.\n");
    }
    if (request.getIntentType() == com.flashsale.aiservice.domain.enums.QuestionIntentType.COMPARE_RECOMMENDATION) {
      prompt.append("- Comparison must be based only on the listed comparison candidates.\n");
    }
    prompt.append("\n");
  }

  /**
   * 添加当前会话的上下文信息（当前商品、上一次意图等）
   */
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

  /**
   * 添加实时事实数据（如价格、库存、秒杀状态）
   */
  private void appendRealtimeFacts(StringBuilder prompt, PromptBuildRequest request) {
    if (!StringUtils.hasText(request.getRealtimeFacts())) {
      return;
    }
    prompt.append("[Realtime facts]\n").append(request.getRealtimeFacts()).append("\n\n");
  }

  /**
   * 添加向量检索到的知识证据片段
   */
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

  /**
   * 添加对比场景下的候选商品列表
   */
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

  /**
   * 添加近期的对话历史（用户提问与助手回答）
   */
  private void appendHistory(StringBuilder prompt, PromptBuildRequest request) {
    if (request.getHistory() == null || request.getHistory().isEmpty()) {
      return;
    }
    prompt.append("[Recent conversation]\n");
    for (ChatRecordPO record : request.getHistory()) {
      if (StringUtils.hasText(record.getQuestion())) {
        prompt.append("User: ").append(record.getQuestion()).append("\n");
      }
      if (StringUtils.hasText(record.getAnswer())) {
        prompt.append("Assistant: ").append(record.getAnswer()).append("\n");
      }
    }
    prompt.append("\n");
  }

  /**
   * 商品事实问答的定制要求
   */
  private void appendProductFactRequirements(StringBuilder prompt) {
    prompt.append("- Focus on the current product only.\n");
    prompt.append("- You may summarize selling points, use cases, target users and value proposition.\n");
    prompt.append("- If the question asks who this product suits, infer cautiously from title, subtitle, detail and features.\n");
    prompt.append("- Keep the answer concrete and directly relevant to the user question.\n");
  }

  /**
   * 实时状态查询的定制要求
   */
  private void appendRealtimeRequirements(StringBuilder prompt) {
    prompt.append("- Answer with realtime facts first.\n");
    prompt.append("- For price, stock or activity status, use only the supplied realtime facts.\n");
    prompt.append("- Do not add speculative recommendations unless clearly supported.\n");
  }

  /**
   * 政策问答的定制要求
   */
  private void appendPolicyRequirements(StringBuilder prompt) {
    prompt.append("- Answer strictly from the supplied policy evidence.\n");
    prompt.append("- Keep the answer explicit and practical.\n");
    prompt.append("- Do not mix in unrelated product descriptions.\n");
  }

  /**
   * 对比推荐场景的定制要求
   */
  private void appendCompareRequirements(StringBuilder prompt) {
    prompt.append("- Produce four short parts in Chinese:\n");
    prompt.append("  1. 当前商品优势\n");
    prompt.append("  2. 当前商品潜在不足\n");
    prompt.append("  3. 适合人群或场景\n");
    prompt.append("  4. 是否建议购买与原因\n");
    prompt.append("- Comparison must only rely on the listed candidates and current product evidence.\n");
    prompt.append("- If comparison evidence is limited, say the recommendation is based on limited current evidence.\n");
    prompt.append("- Avoid generic brand praise or unsupported claims.\n");
  }
}
