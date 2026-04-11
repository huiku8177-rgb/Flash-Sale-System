package com.flashsale.aiservice.service.impl;

import com.flashsale.aiservice.domain.enums.AnswerPolicy;
import com.flashsale.aiservice.domain.vo.RelatedKnowledgeVO;
import com.flashsale.aiservice.service.ChatAuditService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 对话审计服务实现类
 *
 * 负责为每一次对话请求生成结构化的审计摘要（auditSummary），
 * 用于后续的质量分析、问题排查和合规审计。
 *
 * 摘要信息包括：
 * - 回答策略（AnswerPolicy）
 * - 降级原因（若有）
 * - 用户问题（截取前120字符）
 * - 回答预览（截取前120字符）
 * - 命中的知识来源（标题及得分）
 */
@Service
public class ChatAuditServiceImpl implements ChatAuditService {

  /**
   * 构建审计摘要字符串
   *
   * 摘要格式示例：
   * policy=RAG_MODEL, fallbackReason=, question=这款手机怎么样,
   * answerPreview=这款手机采用了最新的..., sources=iPhone 15:0.96, 手机参数:0.88
   *
   * @param question       用户原始问题
   * @param answer         助手生成的回答
   * @param answerPolicy   回答策略（固定模板/RAG模型/降级等）
   * @param fallbackReason 降级原因（若非降级则为空）
   * @param hitKnowledge   检索命中的知识片段列表
   * @return 结构化的审计摘要字符串
   */
  @Override
  public String buildAuditSummary(String question, String answer, AnswerPolicy answerPolicy, String fallbackReason,
                                  List<RelatedKnowledgeVO> hitKnowledge) {
    // 构建知识来源摘要：格式为 "标题:得分, 标题:得分, ..."
    String sourceSummary = hitKnowledge == null || hitKnowledge.isEmpty()
      ? "no knowledge hit"
      : hitKnowledge.stream()
      .map(item -> item.getTitle() + ":" + String.format("%.2f", item.getScore()))
      .collect(Collectors.joining(", "));

    // 截取回答预览（最多120字符）
    String answerPreview = answer == null ? "" : answer.substring(0, Math.min(answer.length(), 120));

    // 拼接最终摘要字符串
    return "policy=" + answerPolicy.name()
      + ", fallbackReason=" + (fallbackReason == null ? "" : fallbackReason)
      + ", question=" + trim(question, 120)
      + ", answerPreview=" + answerPreview
      + ", sources=" + sourceSummary;
  }

  /**
   * 安全截取字符串，若超出最大长度则截断
   *
   * @param value     原始字符串
   * @param maxLength 最大保留长度
   * @return 截取后的字符串，空值返回空字符串
   */
  private String trim(String value, int maxLength) {
    if (value == null) {
      return "";
    }
    return value.length() <= maxLength ? value : value.substring(0, maxLength);
  }
}
