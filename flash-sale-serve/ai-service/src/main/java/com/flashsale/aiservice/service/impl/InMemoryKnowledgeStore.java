package com.flashsale.aiservice.service.impl;

import com.flashsale.aiservice.domain.po.KnowledgeChunkPO;
import com.flashsale.aiservice.domain.po.KnowledgeDocumentPO;
import com.flashsale.aiservice.domain.vo.KnowledgeStatsVO;
import com.flashsale.aiservice.domain.vo.KnowledgeSyncResultVO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryKnowledgeStore {

    private final Map<String, KnowledgeDocumentPO> documents = new ConcurrentHashMap<>();
    private final Map<String, KnowledgeChunkPO> chunks = new ConcurrentHashMap<>();
    private final Map<String, KnowledgeSyncResultVO> syncTasks = new ConcurrentHashMap<>();
    private final AtomicLong totalChatRequests = new AtomicLong();
    private final AtomicLong hitRequests = new AtomicLong();
    private final AtomicLong noResultRequests = new AtomicLong();
    private final AtomicLong fallbackRequests = new AtomicLong();
    private final AtomicLong modelFailures = new AtomicLong();
    private final AtomicLong totalLatencyMs = new AtomicLong();
    private final AtomicLong totalEstimatedTokens = new AtomicLong();
    private volatile LocalDateTime lastSyncAt;

    public void replaceKnowledge(List<KnowledgeDocumentPO> newDocuments, List<KnowledgeChunkPO> newChunks) {
        documents.clear();
        newDocuments.forEach(document -> documents.put(document.getId(), document));
        replaceChunks(newChunks);
        lastSyncAt = LocalDateTime.now();
    }

    public void replaceChunks(List<KnowledgeChunkPO> newChunks) {
        chunks.clear();
        newChunks.forEach(chunk -> chunks.put(chunk.getId(), chunk));
    }

    public List<KnowledgeChunkPO> getAllChunks() {
        return new ArrayList<>(chunks.values());
    }

    public void saveSyncTask(KnowledgeSyncResultVO task) {
        syncTasks.put(task.getTaskId(), task);
    }

    public KnowledgeSyncResultVO getSyncTask(String taskId) {
        return syncTasks.get(taskId);
    }

    public void incrementChatRequests() {
        totalChatRequests.incrementAndGet();
    }

    public void incrementHitRequests() {
        hitRequests.incrementAndGet();
    }

    public void incrementNoResult() {
        noResultRequests.incrementAndGet();
    }

    public void incrementFallbacks() {
        fallbackRequests.incrementAndGet();
    }

    public void incrementModelFailures() {
        modelFailures.incrementAndGet();
    }

    public void recordLatency(long latencyMs, long estimatedTokens) {
        totalLatencyMs.addAndGet(latencyMs);
        totalEstimatedTokens.addAndGet(estimatedTokens);
    }

    public KnowledgeStatsVO buildStats(long sessionCount, long chatRecordCount) {
        KnowledgeStatsVO stats = new KnowledgeStatsVO();
        long totalRequests = totalChatRequests.get();
        stats.setDocumentCount(documents.size());
        stats.setChunkCount(chunks.size());
        stats.setSessionCount((int) sessionCount);
        stats.setChatRecordCount((int) chatRecordCount);
        stats.setTotalChatRequests(totalRequests);
        stats.setKnowledgeHitRate(rate(hitRequests.get(), totalRequests));
        stats.setNoResultRate(rate(noResultRequests.get(), totalRequests));
        stats.setFallbackRate(rate(fallbackRequests.get(), totalRequests));
        stats.setModelFailureRate(rate(modelFailures.get(), totalRequests));
        stats.setAvgLatencyMs(rate(totalLatencyMs.get(), totalRequests));
        stats.setAvgEstimatedTokens(rate(totalEstimatedTokens.get(), totalRequests));
        stats.setLastSyncAt(lastSyncAt);
        return stats;
    }

    private double rate(long numerator, long denominator) {
        return denominator == 0 ? 0d : (double) numerator / denominator;
    }
}
