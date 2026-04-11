package com.flashsale.aiservice.service.impl;

import com.flashsale.aiservice.client.ProductKnowledgeClient;
import com.flashsale.aiservice.client.SeckillKnowledgeClient;
import com.flashsale.aiservice.config.AiProperties;
import com.flashsale.aiservice.domain.dto.KnowledgeSyncRequestDTO;
import com.flashsale.aiservice.domain.dto.ProductKnowledgeDTO;
import com.flashsale.aiservice.domain.dto.SeckillKnowledgeDTO;
import com.flashsale.aiservice.domain.enums.KnowledgeSourceType;
import com.flashsale.aiservice.domain.enums.SyncStatus;
import com.flashsale.aiservice.domain.po.KnowledgeChunkPO;
import com.flashsale.aiservice.domain.po.KnowledgeDocumentPO;
import com.flashsale.aiservice.domain.vo.KnowledgeStatsVO;
import com.flashsale.aiservice.domain.vo.KnowledgeSyncResultVO;
import com.flashsale.aiservice.exception.KnowledgeSyncException;
import com.flashsale.aiservice.service.ChatRecordService;
import com.flashsale.aiservice.service.ChatSessionService;
import com.flashsale.aiservice.service.DocumentChunkService;
import com.flashsale.aiservice.service.KnowledgeSyncService;
import com.flashsale.aiservice.service.VectorStoreService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 知识库同步服务实现类
 *
 * 负责从外部数据源（商品服务、秒杀服务、配置规则）拉取知识数据，
 * 进行文档分块后写入向量存储，并更新内存知识库缓存。
 *
 * 同步的数据类型包括：
 * - 普通商品（PRODUCT）
 * - 秒杀商品（SECKILL）
 * - 规则文档（RULE）
 *
 * 注意：价格、库存、活动时间等实时数据不会被写入向量库，
 * 仅将稳定的商品描述文本（如名称、详情、卖点）用于向量检索。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeSyncServiceImpl implements KnowledgeSyncService {

  // 依赖注入
  private final ProductKnowledgeClient productKnowledgeClient;   // 普通商品数据客户端
  private final SeckillKnowledgeClient seckillKnowledgeClient;   // 秒杀商品数据客户端
  private final DocumentChunkService documentChunkService;       // 文档分块服务
  private final VectorStoreService vectorStoreService;           // 向量存储服务
  private final InMemoryKnowledgeStore knowledgeStore;           // 内存知识库缓存
  private final ChatSessionService chatSessionService;           // 会话服务（用于统计）
  private final ChatRecordService chatRecordService;             // 对话记录服务（用于统计）
  private final AiProperties aiProperties;                       // AI配置属性

  /**
   * 执行知识库同步
   *
   * 流程：
   * 1. 创建同步任务记录并标记为运行中
   * 2. 根据请求参数构建文档列表（商品 + 规则）
   * 3. 对文档进行分块处理
   * 4. 替换向量存储中的所有数据
   * 5. 更新内存知识库缓存
   * 6. 记录同步结果
   *
   * @param request 同步请求参数（可指定仅同步某个商品ID，或全量同步）
   * @return 同步结果VO，包含任务ID、状态、同步数量等
   * @throws KnowledgeSyncException 同步过程中发生异常时抛出
   */
  @Override
  public KnowledgeSyncResultVO sync(KnowledgeSyncRequestDTO request) {
    KnowledgeSyncResultVO result = new KnowledgeSyncResultVO();
    result.setTaskId(UUID.randomUUID().toString());
    result.setSyncType(request.getSyncType());
    result.setStatus(SyncStatus.RUNNING);
    result.setStartedAt(LocalDateTime.now());
    knowledgeStore.saveSyncTask(result);

    try {
      // 构建文档列表
      List<KnowledgeDocumentPO> documents = buildDocuments(request);
      List<KnowledgeChunkPO> chunks = new ArrayList<>();

      // 对每个文档进行分块
      for (KnowledgeDocumentPO document : documents) {
        chunks.addAll(documentChunkService.chunk(document));
      }

      log.info("Knowledge sync built documents={}, chunks={}, syncType={}",
        documents.size(), chunks.size(), request.getSyncType());

      // 全量替换向量存储
      vectorStoreService.replaceAll(chunks);
      // 更新内存缓存
      knowledgeStore.replaceKnowledge(documents, chunks);

      result.setStatus(SyncStatus.SUCCESS);
      result.setMessage("知识同步完成");
      result.setSyncedDocuments(documents.size());
      result.setSyncedChunks(chunks.size());
      result.setFinishedAt(LocalDateTime.now());
      knowledgeStore.saveSyncTask(result);
      return result;
    } catch (Exception ex) {
      result.setStatus(SyncStatus.FAILED);
      result.setMessage("知识同步失败: " + ex.getMessage());
      result.setFinishedAt(LocalDateTime.now());
      knowledgeStore.markKnowledgeNotReady(result.getMessage());
      knowledgeStore.saveSyncTask(result);
      throw new KnowledgeSyncException(result.getMessage(), ex);
    }
  }

  /**
   * 根据任务ID查询同步任务状态
   *
   * @param taskId 任务ID
   * @return 同步任务结果
   * @throws KnowledgeSyncException 若任务不存在
   */
  @Override
  public KnowledgeSyncResultVO getTask(String taskId) {
    KnowledgeSyncResultVO task = knowledgeStore.getSyncTask(taskId);
    if (task == null) {
      throw new KnowledgeSyncException("未找到同步任务: " + taskId);
    }
    return task;
  }

  /**
   * 获取知识库统计信息
   *
   * @return 包含文档数、分块数、会话数、对话记录数等统计信息
   */
  @Override
  public KnowledgeStatsVO getStats() {
    return knowledgeStore.buildStats(chatSessionService.countSessions(), chatRecordService.countRecords());
  }

  /**
   * 构建待同步的文档列表
   *
   * 数据来源：
   * - 普通商品（若请求指定了 productId 则只同步该商品）
   * - 秒杀商品（全量同步）
   * - 配置中的规则文档
   *
   * 关键设计：仅将稳定的商品描述文本（toRetrievalText）写入文档，
   * 价格、库存、秒杀时间等实时数据不进入向量库，避免过时信息污染检索结果。
   *
   * @param request 同步请求
   * @return 文档列表
   */
  private List<KnowledgeDocumentPO> buildDocuments(KnowledgeSyncRequestDTO request) {
    List<KnowledgeDocumentPO> documents = new ArrayList<>();
    int productCount = 0;
    int seckillCount = 0;
    int ruleCount = 0;

    // 普通商品同步（全量或单个）
    if (request.getProductId() != null) {
      ProductKnowledgeDTO product = productKnowledgeClient.getProductById(request.getProductId());
      if (product != null) {
        // 只有稳定的检索文本才允许进入向量文档
        // 价格、库存等实时数据不写入向量语料库
        documents.add(toDocument(KnowledgeSourceType.PRODUCT, String.valueOf(product.getId()), product.getName(), product.toRetrievalText()));
        productCount++;
      }
    } else {
      for (ProductKnowledgeDTO product : productKnowledgeClient.getAllProducts()) {
        // 保持同步语料的确定性和稳定性
        documents.add(toDocument(KnowledgeSourceType.PRODUCT, String.valueOf(product.getId()), product.getName(), product.toRetrievalText()));
        productCount++;
      }
    }

    // 秒杀商品同步（全量，仅同步稳定描述，不含实时价格/时间）
    for (SeckillKnowledgeDTO seckill : seckillKnowledgeClient.getAllProducts()) {
      // 秒杀开始/结束时间和价格属于实时事实，禁止混入向量库
      documents.add(toDocument(KnowledgeSourceType.SECKILL, String.valueOf(seckill.getId()), seckill.getName(), seckill.toRetrievalText()));
      seckillCount++;
    }

    // 规则文档同步
    int ruleIndex = 0;
    for (AiProperties.RuleDocumentProperties rule : aiProperties.getRuleDocuments()) {
      documents.add(toDocument(KnowledgeSourceType.RULE, "rule-" + ruleIndex++, rule.getTitle(), rule.getContent()));
      ruleCount++;
    }

    // 日志警告（便于排查数据源配置问题）
    if (productCount == 0) {
      log.warn("No product documents were loaded during knowledge sync, productServiceUrl={}", aiProperties.getProductServiceUrl());
    }
    if (seckillCount == 0) {
      log.warn("No seckill documents were loaded during knowledge sync, seckillServiceUrl={}", aiProperties.getSeckillServiceUrl());
    }
    if (ruleCount == 0) {
      log.warn("No rule documents were configured for knowledge sync");
    }

    log.info("Knowledge sync sources loaded: productDocuments={}, seckillDocuments={}, ruleDocuments={}",
      productCount, seckillCount, ruleCount);
    return documents;
  }

  /**
   * 将数据源条目转换为知识文档对象
   *
   * @param sourceType 来源类型（PRODUCT / SECKILL / RULE）
   * @param sourceId   来源ID
   * @param title      文档标题
   * @param content    文档内容（用于分块和向量化）
   * @return 知识文档PO
   * @throws KnowledgeSyncException 若内容为空
   */
  private KnowledgeDocumentPO toDocument(KnowledgeSourceType sourceType, String sourceId, String title, String content) {
    if (content == null || content.isBlank()) {
      throw new KnowledgeSyncException("知识内容为空, sourceId=" + sourceId);
    }
    KnowledgeDocumentPO document = new KnowledgeDocumentPO();
    // 文档ID格式：类型-来源ID，便于去重和更新
    document.setId(sourceType.name().toLowerCase() + "-" + sourceId);
    document.setSourceType(sourceType);
    document.setSourceId(sourceId);
    document.setTitle(title);
    document.setContent(content);
    document.setUpdatedAt(LocalDateTime.now());
    return document;
  }
}
