package com.flashsale.aiservice.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "知识库统计信息")
public class KnowledgeStatsVO {

    @Schema(description = "文档总数", example = "128")
    private int documentCount;

    @Schema(description = "分块总数", example = "640")
    private int chunkCount;

    @Schema(description = "知识库是否就绪", example = "true")
    private boolean knowledgeReady;

    @Schema(description = "会话总数", example = "24")
    private int sessionCount;

    @Schema(description = "聊天记录总数", example = "386")
    private int chatRecordCount;

    @Schema(description = "总请求数", example = "520")
    private long totalChatRequests;

    @Schema(description = "知识命中率", example = "0.78")
    private double knowledgeHitRate;

    @Schema(description = "无结果率", example = "0.12")
    private double noResultRate;

    @Schema(description = "降级率", example = "0.08")
    private double fallbackRate;

    @Schema(description = "模型失败率", example = "0.03")
    private double modelFailureRate;

    @Schema(description = "平均延迟毫秒数", example = "186.5")
    private double avgLatencyMs;

    @Schema(description = "平均估算 token 数", example = "420.0")
    private double avgEstimatedTokens;

    @Schema(description = "最近一次同步时间")
    private LocalDateTime lastSyncAt;

    @Schema(description = "最近一次同步状态", example = "READY")
    private String lastSyncStatus;

    @Schema(description = "最近一次同步说明", example = "Knowledge sync completed successfully")
    private String lastSyncMessage;
}
