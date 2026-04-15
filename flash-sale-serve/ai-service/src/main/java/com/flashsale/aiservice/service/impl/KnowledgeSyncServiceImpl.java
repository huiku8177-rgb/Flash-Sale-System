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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeSyncServiceImpl implements KnowledgeSyncService {

  private final ProductKnowledgeClient productKnowledgeClient;
  private final SeckillKnowledgeClient seckillKnowledgeClient;
  private final DocumentChunkService documentChunkService;
  private final VectorStoreService vectorStoreService;
  private final InMemoryKnowledgeStore knowledgeStore;
  private final ChatSessionService chatSessionService;
  private final ChatRecordService chatRecordService;
  private final AiProperties aiProperties;

  @Override
  public KnowledgeSyncResultVO sync(KnowledgeSyncRequestDTO request) {
    KnowledgeSyncRequestDTO actualRequest = request == null ? new KnowledgeSyncRequestDTO() : request;
    KnowledgeSyncResultVO result = new KnowledgeSyncResultVO();
    result.setTaskId(UUID.randomUUID().toString());
    result.setSyncType(actualRequest.getSyncType());
    result.setStatus(SyncStatus.RUNNING);
    result.setStartedAt(LocalDateTime.now());
    knowledgeStore.saveSyncTask(result);

    try {
      List<KnowledgeDocumentPO> documents = buildDocuments(actualRequest);
      List<KnowledgeChunkPO> chunks = new ArrayList<>();
      int failedDocuments = 0;

      for (KnowledgeDocumentPO document : documents) {
        try {
          List<KnowledgeChunkPO> documentChunks = documentChunkService.chunk(document);
          if (documentChunks.isEmpty()) {
            log.warn("Skip knowledge document without chunk output, documentId={}, sourceType={}, title={}",
              document.getId(), document.getSourceType(), document.getTitle());
            failedDocuments++;
            continue;
          }
          chunks.addAll(documentChunks);
        } catch (Exception ex) {
          failedDocuments++;
          log.warn("Skip knowledge document during sync, documentId={}, sourceType={}, title={}, reason={}",
            document.getId(), document.getSourceType(), document.getTitle(), ex.getMessage(), ex);
        }
      }

      log.info("Knowledge sync built documents={}, chunks={}, failedDocuments={}, syncType={}",
        documents.size(), chunks.size(), failedDocuments, actualRequest.getSyncType());

      if (chunks.isEmpty()) {
        throw new KnowledgeSyncException("Knowledge sync produced no chunks");
      }

      vectorStoreService.replaceAll(chunks);
      knowledgeStore.replaceKnowledge(documents, chunks);

      result.setStatus(SyncStatus.SUCCESS);
      result.setMessage(failedDocuments == 0 ? "知识同步完成" : "知识同步完成，部分文档已跳过");
      result.setSyncedDocuments(documents.size() - failedDocuments);
      result.setSyncedChunks(chunks.size());
      result.setFinishedAt(LocalDateTime.now());
      knowledgeStore.saveSyncTask(result);
      return result;
    } catch (Exception ex) {
      result.setStatus(SyncStatus.FAILED);
      result.setMessage("知识同步失败: " + ex.getMessage());
      result.setFinishedAt(LocalDateTime.now());
      knowledgeStore.markSyncFailed(result.getMessage());
      knowledgeStore.saveSyncTask(result);
      throw new KnowledgeSyncException(result.getMessage(), ex);
    }
  }

  @Override
  public KnowledgeSyncResultVO getTask(String taskId) {
    KnowledgeSyncResultVO task = knowledgeStore.getSyncTask(taskId);
    if (task == null) {
      throw new KnowledgeSyncException("未找到同步任务: " + taskId);
    }
    return task;
  }

  @Override
  public KnowledgeStatsVO getStats() {
    return knowledgeStore.buildStats(chatSessionService.countSessions(), chatRecordService.countRecords());
  }

  private List<KnowledgeDocumentPO> buildDocuments(KnowledgeSyncRequestDTO request) {
    List<KnowledgeDocumentPO> documents = new ArrayList<>();
    int productCount = 0;
    int seckillCount = 0;
    int ruleCount = 0;

    if (request.getProductId() != null) {
      ProductKnowledgeDTO product = productKnowledgeClient.getProductById(request.getProductId());
      if (product != null) {
        KnowledgeDocumentPO document = toDocumentOrNull(KnowledgeSourceType.PRODUCT, String.valueOf(product.getId()), product.getName(), product.toRetrievalText());
        if (document != null) {
          documents.add(document);
          productCount++;
        }
      }
    } else {
      for (ProductKnowledgeDTO product : productKnowledgeClient.getAllProducts()) {
        KnowledgeDocumentPO document = toDocumentOrNull(KnowledgeSourceType.PRODUCT, String.valueOf(product.getId()), product.getName(), product.toRetrievalText());
        if (document != null) {
          documents.add(document);
          productCount++;
        }
      }
    }

    for (SeckillKnowledgeDTO seckill : seckillKnowledgeClient.getAllProducts()) {
      KnowledgeDocumentPO document = toDocumentOrNull(KnowledgeSourceType.SECKILL, String.valueOf(seckill.getId()), seckill.getName(), seckill.toRetrievalText());
      if (document != null) {
        documents.add(document);
        seckillCount++;
      }
    }

    int ruleIndex = 0;
    for (AiProperties.RuleDocumentProperties rule : aiProperties.getRuleDocuments()) {
      KnowledgeDocumentPO document = toDocumentOrNull(KnowledgeSourceType.RULE, "rule-" + ruleIndex, rule.getTitle(), rule.getContent());
      ruleIndex++;
      if (document != null) {
        documents.add(document);
        ruleCount++;
      }
    }

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

  private KnowledgeDocumentPO toDocumentOrNull(KnowledgeSourceType sourceType, String sourceId, String title, String content) {
    if (!StringUtils.hasText(content)) {
      log.warn("Skip empty knowledge content during sync, sourceType={}, sourceId={}, title={}", sourceType, sourceId, title);
      return null;
    }
    KnowledgeDocumentPO document = new KnowledgeDocumentPO();
    document.setId(sourceType.name().toLowerCase() + "-" + sourceId);
    document.setSourceType(sourceType);
    document.setSourceId(sourceId);
    document.setTitle(title);
    document.setContent(content);
    document.setUpdatedAt(LocalDateTime.now());
    return document;
  }
}
