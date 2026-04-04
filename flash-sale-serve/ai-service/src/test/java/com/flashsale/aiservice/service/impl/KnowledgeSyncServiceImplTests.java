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
import com.flashsale.aiservice.service.ChatRecordService;
import com.flashsale.aiservice.service.ChatSessionService;
import com.flashsale.aiservice.service.DocumentChunkService;
import com.flashsale.aiservice.service.VectorStoreService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

class KnowledgeSyncServiceImplTests {

    @Test
    void syncBuildsDocumentsAndUpdatesStats() {
        ProductKnowledgeClient productClient = Mockito.mock(ProductKnowledgeClient.class);
        ProductKnowledgeDTO product = new ProductKnowledgeDTO();
        product.setId(1001L);
        product.setName("测试商品");
        product.setPrice(new BigDecimal("88.00"));
        Mockito.when(productClient.getAllProducts()).thenReturn(List.of(product));

        SeckillKnowledgeClient seckillClient = Mockito.mock(SeckillKnowledgeClient.class);
        SeckillKnowledgeDTO seckill = new SeckillKnowledgeDTO();
        seckill.setId(2001L);
        seckill.setName("秒杀商品");
        seckill.setSeckillPrice(new BigDecimal("66.00"));
        Mockito.when(seckillClient.getAllProducts()).thenReturn(List.of(seckill));

        DocumentChunkService documentChunkService = Mockito.mock(DocumentChunkService.class);
        Mockito.when(documentChunkService.chunk(any())).thenAnswer(invocation -> {
            KnowledgeDocumentPO document = invocation.getArgument(0);
            KnowledgeChunkPO chunk = new KnowledgeChunkPO();
            chunk.setId(document.getId() + "-chunk-0");
            chunk.setDocumentId(document.getId());
            chunk.setSourceType(document.getSourceType());
            chunk.setSourceId(document.getSourceId());
            chunk.setTitle(document.getTitle());
            chunk.setContent(document.getContent());
            chunk.setEmbedding(List.of(0.1, 0.2));
            return List.of(chunk);
        });

        InMemoryKnowledgeStore store = new InMemoryKnowledgeStore();
        VectorStoreService vectorStoreService = new VectorStoreServiceImpl(store);
        ChatSessionService chatSessionService = Mockito.mock(ChatSessionService.class);
        ChatRecordService chatRecordService = Mockito.mock(ChatRecordService.class);
        Mockito.when(chatSessionService.countSessions()).thenReturn(2L);
        Mockito.when(chatRecordService.countRecords()).thenReturn(5L);

        AiProperties properties = new AiProperties();
        AiProperties.RuleDocumentProperties rule = new AiProperties.RuleDocumentProperties();
        rule.setTitle("售后政策");
        rule.setSourceType(KnowledgeSourceType.RULE.name());
        rule.setContent("支持七天无理由退货");
        properties.setRuleDocuments(List.of(rule));

        KnowledgeSyncServiceImpl service = new KnowledgeSyncServiceImpl(
                productClient,
                seckillClient,
                documentChunkService,
                vectorStoreService,
                store,
                chatSessionService,
                chatRecordService,
                properties
        );

        KnowledgeSyncResultVO result = service.sync(new KnowledgeSyncRequestDTO());
        KnowledgeStatsVO stats = service.getStats();

        assertEquals(SyncStatus.SUCCESS, result.getStatus());
        assertEquals(3, result.getSyncedDocuments());
        assertEquals(3, result.getSyncedChunks());
        assertEquals(3, stats.getDocumentCount());
        assertEquals(3, stats.getChunkCount());
        assertEquals(2, stats.getSessionCount());
        assertEquals(5, stats.getChatRecordCount());
        assertNotNull(service.getTask(result.getTaskId()));
    }
}
