package com.flashsale.aiservice.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "命中的相关知识")
public class RelatedKnowledgeVO {

    @Schema(description = "文档 ID", example = "doc-1001")
    private String documentId;

    @Schema(description = "知识标题", example = "售后政策")
    private String title;

    @Schema(description = "来源类型", example = "RULE")
    private String sourceType;

    @Schema(description = "来源业务 ID", example = "1001")
    private String sourceId;

    @Schema(description = "知识片段")
    private String snippet;

    @Schema(description = "相似度得分", example = "0.88")
    private double score;

    @Schema(description = "是否来自实时查询", example = "true")
    private boolean realtime;
}
