package com.flashsale.aiservice.domain.po;

import com.flashsale.aiservice.domain.enums.SyncStatus;
import com.flashsale.aiservice.domain.enums.SyncType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeSyncTaskPO {

    private String taskId;
    private SyncType syncType;
    private SyncStatus status;
    private String message;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
