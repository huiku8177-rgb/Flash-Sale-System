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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        KnowledgeSyncResultVO result = new KnowledgeSyncResultVO();
        result.setTaskId(UUID.randomUUID().toString());
        result.setSyncType(request.getSyncType());
        result.setStatus(SyncStatus.RUNNING);
        result.setStartedAt(LocalDateTime.now());
        knowledgeStore.saveSyncTask(result);

        try {
            List<KnowledgeDocumentPO> documents = buildDocuments(request);
            List<KnowledgeChunkPO> chunks = new ArrayList<>();
            for (KnowledgeDocumentPO document : documents) {
                chunks.addAll(documentChunkService.chunk(document));
            }

            vectorStoreService.replaceAll(chunks);
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

        if (request.getProductId() != null) {
            ProductKnowledgeDTO product = productKnowledgeClient.getProductById(request.getProductId());
            if (product != null) {
                documents.add(toDocument(KnowledgeSourceType.PRODUCT, String.valueOf(product.getId()), product.getName(), product.toKnowledgeText()));
            }
        } else {
            for (ProductKnowledgeDTO product : productKnowledgeClient.getAllProducts()) {
                documents.add(toDocument(KnowledgeSourceType.PRODUCT, String.valueOf(product.getId()), product.getName(), product.toKnowledgeText()));
            }
        }

        for (SeckillKnowledgeDTO seckill : seckillKnowledgeClient.getAllProducts()) {
            documents.add(toDocument(KnowledgeSourceType.SECKILL, String.valueOf(seckill.getId()), seckill.getName(), seckill.toKnowledgeText()));
        }

        int ruleIndex = 0;
        for (AiProperties.RuleDocumentProperties rule : aiProperties.getRuleDocuments()) {
            documents.add(toDocument(KnowledgeSourceType.RULE, "rule-" + ruleIndex++, rule.getTitle(), rule.getContent()));
        }
        return documents;
    }

    private KnowledgeDocumentPO toDocument(KnowledgeSourceType sourceType, String sourceId, String title, String content) {
        if (content == null || content.isBlank()) {
            throw new KnowledgeSyncException("知识内容为空, sourceId=" + sourceId);
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
