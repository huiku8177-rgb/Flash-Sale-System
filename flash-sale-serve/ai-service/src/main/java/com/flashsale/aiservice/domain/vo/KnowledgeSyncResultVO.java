package com.flashsale.aiservice.domain.vo;

import com.flashsale.aiservice.domain.enums.SyncStatus;
import com.flashsale.aiservice.domain.enums.SyncType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "知识同步结果")
public class KnowledgeSyncResultVO {

    @Schema(description = "同步任务 ID", example = "task-001")
    private String taskId;

    @Schema(description = "同步类型", example = "FULL")
    private SyncType syncType;

    @Schema(description = "同步状态", example = "SUCCESS")
    private SyncStatus status;

    @Schema(description = "状态说明", example = "Knowledge sync completed successfully")
    private String message;

    @Schema(description = "已同步文档数", example = "32")
    private int syncedDocuments;

    @Schema(description = "已同步分块数", example = "160")
    private int syncedChunks;

    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    @Schema(description = "结束时间")
    private LocalDateTime finishedAt;
}
