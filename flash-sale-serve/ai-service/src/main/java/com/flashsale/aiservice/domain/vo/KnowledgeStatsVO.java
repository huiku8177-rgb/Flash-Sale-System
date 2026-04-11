package com.flashsale.aiservice.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeStatsVO {

    private int documentCount;
    private int chunkCount;
    private boolean knowledgeReady;
    private int sessionCount;
    private int chatRecordCount;
    private long totalChatRequests;
    private double knowledgeHitRate;
    private double noResultRate;
    private double fallbackRate;
    private double modelFailureRate;
    private double avgLatencyMs;
    private double avgEstimatedTokens;
    private LocalDateTime lastSyncAt;
    private String lastSyncStatus;
    private String lastSyncMessage;
}
