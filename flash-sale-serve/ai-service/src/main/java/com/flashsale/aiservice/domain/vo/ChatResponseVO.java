package com.flashsale.aiservice.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "AI 问答响应结果")
public class ChatResponseVO {

    @Schema(description = "回答内容", example = "支持七天无理由退货，具体以商品详情页说明为准。")
    private String answer;

    @Schema(description = "来源标题列表")
    private List<String> sources;

    @Schema(description = "命中知识详情")
    private List<RelatedKnowledgeVO> hitKnowledge;

    @Schema(description = "置信度", example = "0.82")
    private double confidence;

    @Schema(description = "降级或拒答原因", example = "NO_RELEVANT_KNOWLEDGE")
    private String fallbackReason;

    @Schema(description = "回答策略", example = "RAG_MODEL")
    private String answerPolicy;

    @Schema(description = "会话 ID", example = "session-001")
    private String sessionId;

    @Schema(description = "问题分类", example = "AFTER_SALES_POLICY")
    private String category;
}
