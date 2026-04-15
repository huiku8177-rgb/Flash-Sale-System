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

/**
 * 内存知识库存储组件
 *
 * 作为知识库在内存中的缓存层，提供以下核心能力：
 * 1. 存储知识文档及其分块，支持全量替换和查询
 * 2. 管理知识同步任务的状态记录
 * 3. 收集系统运行指标（请求量、命中率、延迟、Token消耗等）
 * 4. 维护知识库的就绪状态及最后一次同步信息
 *
 * 该类采用线程安全的 ConcurrentHashMap 和 AtomicLong，适用于高并发场景。
 * 主要用于替代频繁的外部向量库查询，提升检索性能和系统可观测性。
 */
@Component
public class InMemoryKnowledgeStore {

  // 知识文档存储：Key = 文档ID，Value = 文档对象
  private final Map<String, KnowledgeDocumentPO> documents = new ConcurrentHashMap<>();

  // 知识分块存储：Key = 分块ID，Value = 分块对象
  private final Map<String, KnowledgeChunkPO> chunks = new ConcurrentHashMap<>();

  // 同步任务记录：Key = 任务ID，Value = 同步结果
  private final Map<String, KnowledgeSyncResultVO> syncTasks = new ConcurrentHashMap<>();

  // 统计指标：总对话请求数
  private final AtomicLong totalChatRequests = new AtomicLong();

  // 统计指标：检索命中次数（有相关知识返回）
  private final AtomicLong hitRequests = new AtomicLong();

  // 统计指标：无结果次数（检索无相关内容）
  private final AtomicLong noResultRequests = new AtomicLong();

  // 统计指标：触发降级次数
  private final AtomicLong fallbackRequests = new AtomicLong();

  // 统计指标：大模型调用失败次数
  private final AtomicLong modelFailures = new AtomicLong();

  // 统计指标：累计延迟（毫秒）
  private final AtomicLong totalLatencyMs = new AtomicLong();

  // 统计指标：累计估算 Token 数
  private final AtomicLong totalEstimatedTokens = new AtomicLong();

  // 知识库就绪标志（volatile 保证多线程可见性）
  private volatile boolean knowledgeReady;

  // 最后一次同步时间
  private volatile LocalDateTime lastSyncAt;

  // 最后一次同步状态
  private volatile String lastSyncStatus = "NOT_READY";

  // 最后一次同步信息（成功或失败原因）
  private volatile String lastSyncMessage = "Knowledge base has not been initialized";

  /**
   * 全量替换知识库
   *
   * 清空现有文档和分块，加载新数据，并更新同步状态。
   *
   * @param newDocuments 新的文档列表
   * @param newChunks    新的分块列表
   */
  public void replaceKnowledge(List<KnowledgeDocumentPO> newDocuments, List<KnowledgeChunkPO> newChunks) {
    documents.clear();
    newDocuments.forEach(document -> documents.put(document.getId(), document));
    replaceChunks(newChunks);
    lastSyncAt = LocalDateTime.now();
    knowledgeReady = !chunks.isEmpty();
    lastSyncStatus = knowledgeReady ? "READY" : "EMPTY";
    lastSyncMessage = knowledgeReady
      ? "Knowledge base is ready for retrieval"
      : "Knowledge sync finished but no chunks were loaded";
  }

  /**
   * 替换知识分块（保留文档不变）
   *
   * @param newChunks 新的分块列表
   */
  public void replaceChunks(List<KnowledgeChunkPO> newChunks) {
    chunks.clear();
    newChunks.forEach(chunk -> chunks.put(chunk.getId(), chunk));
  }

  /**
   * 获取所有知识分块
   *
   * 返回副本以避免并发修改问题。
   *
   * @return 所有分块的列表
   */
  public List<KnowledgeChunkPO> getAllChunks() {
    return new ArrayList<>(chunks.values());
  }

  /**
   * 检查知识库是否已就绪（可用于检索）
   */
  public boolean isKnowledgeReady() {
    return knowledgeReady;
  }

  public boolean hasKnowledgeSnapshot() {
    return !chunks.isEmpty();
  }

  /**
   * 标记知识库为未就绪状态（通常在同步失败时调用）
   *
   * @param message 失败原因或描述信息
   */
  public void markKnowledgeNotReady(String message) {
    knowledgeReady = false;
    lastSyncStatus = "NOT_READY";
    lastSyncMessage = message;
  }

  public void markSyncFailed(String message) {
    if (hasKnowledgeSnapshot()) {
      lastSyncStatus = "STALE_READY";
      lastSyncMessage = message;
      return;
    }
    markKnowledgeNotReady(message);
  }

  /**
   * 保存同步任务结果
   *
   * @param task 同步任务结果对象
   */
  public void saveSyncTask(KnowledgeSyncResultVO task) {
    syncTasks.put(task.getTaskId(), task);
  }

  /**
   * 根据任务ID获取同步任务结果
   *
   * @param taskId 任务ID
   * @return 同步任务结果，不存在则返回 null
   */
  public KnowledgeSyncResultVO getSyncTask(String taskId) {
    return syncTasks.get(taskId);
  }

  // ==================== 指标收集方法 ====================

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

  /**
   * 记录一次请求的延迟和 Token 消耗
   *
   * @param latencyMs       处理耗时（毫秒）
   * @param estimatedTokens 估算的 Token 数量
   */
  public void recordLatency(long latencyMs, long estimatedTokens) {
    totalLatencyMs.addAndGet(latencyMs);
    totalEstimatedTokens.addAndGet(estimatedTokens);
  }

  /**
   * 构建知识库统计信息视图
   *
   * 聚合所有内存中的计数器和状态，生成供外部展示或监控的统计数据。
   *
   * @param sessionCount    总会话数（由外部传入）
   * @param chatRecordCount 总对话记录数（由外部传入）
   * @return 统计信息 VO
   */
  public KnowledgeStatsVO buildStats(long sessionCount, long chatRecordCount) {
    KnowledgeStatsVO stats = new KnowledgeStatsVO();
    long totalRequests = totalChatRequests.get();
    stats.setDocumentCount(documents.size());
    stats.setChunkCount(chunks.size());
    stats.setKnowledgeReady(knowledgeReady);
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
    stats.setLastSyncStatus(lastSyncStatus);
    stats.setLastSyncMessage(lastSyncMessage);
    return stats;
  }

  /**
   * 计算比率，避免除零错误
   *
   * @param numerator   分子
   * @param denominator 分母
   * @return 比率（分母为 0 时返回 0）
   */
  private double rate(long numerator, long denominator) {
    return denominator == 0 ? 0d : (double) numerator / denominator;
  }
}
