package com.flashsale.aiservice.domain.vo;

import com.flashsale.aiservice.domain.enums.SyncStatus;
import com.flashsale.aiservice.domain.enums.SyncType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeSyncResultVO {

    private String taskId;
    private SyncType syncType;
    private SyncStatus status;
    private String message;
    private int syncedDocuments;
    private int syncedChunks;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
