package com.flashsale.aiservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.aiservice.client.ChatModelClient;
import com.flashsale.aiservice.client.EmbeddingClient;
import com.flashsale.aiservice.client.ProductKnowledgeClient;
import com.flashsale.aiservice.client.SeckillKnowledgeClient;
import com.flashsale.aiservice.config.AiProperties;
import com.flashsale.aiservice.domain.dto.ChatRequestDTO;
import com.flashsale.aiservice.domain.dto.ProductKnowledgeDTO;
import com.flashsale.aiservice.domain.enums.AnswerPolicy;
import com.flashsale.aiservice.domain.enums.KnowledgeSourceType;
import com.flashsale.aiservice.domain.enums.QuestionCategory;
import com.flashsale.aiservice.domain.po.ChatRecordPO;
import com.flashsale.aiservice.domain.po.ChatSessionPO;
import com.flashsale.aiservice.domain.po.KnowledgeChunkPO;
import com.flashsale.aiservice.domain.vo.ChatResponseVO;
import com.flashsale.aiservice.domain.vo.RelatedKnowledgeVO;
import com.flashsale.aiservice.exception.ModelInvokeException;
import com.flashsale.aiservice.service.ChatAuditService;
import com.flashsale.aiservice.service.ChatRecordService;
import com.flashsale.aiservice.service.ChatSessionService;
import com.flashsale.aiservice.service.KnowledgeRetrievalService;
import com.flashsale.aiservice.service.PromptBuilderService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class RagChatServiceImplTests {

    @Test
    void chatFallsBackWhenNoRelevantKnowledgeExists() {
        RagChatServiceImpl service = createService(false, List.of(), null);
        ChatRequestDTO request = new ChatRequestDTO();
        request.setQuestion("\u652f\u6301\u9000\u6b3e\u5417");

        ChatResponseVO result = service.chat(10001L, request);

        assertEquals("NO_RELEVANT_KNOWLEDGE", result.getFallbackReason());
        assertEquals(QuestionCategory.AFTER_SALES_POLICY.name(), result.getCategory());
        assertEquals(AnswerPolicy.RAG_FALLBACK_NO_KNOWLEDGE.name(), result.getAnswerPolicy());
        assertTrue(result.getAnswer().contains("\u552e\u540e"));
    }

    @Test
    void chatUsesRealtimeFactsForSensitiveQuestions() {
        ProductKnowledgeDTO product = new ProductKnowledgeDTO();
        product.setId(1001L);
        product.setPrice(new BigDecimal("99.90"));
        product.setStock(12);
        product.setStatus(1);

        RagChatServiceImpl service = createService(false, List.of(mockKnowledge(0.8d)), product);
        ChatRequestDTO request = new ChatRequestDTO();
        request.setQuestion("\u8fd9\u4e2a\u5546\u54c1\u73b0\u5728\u591a\u5c11\u94b1");
        request.setProductId(1001L);

        ChatResponseVO result = service.chat(10001L, request);

        assertTrue(result.getHitKnowledge().stream().anyMatch(RelatedKnowledgeVO::isRealtime));
        assertEquals(AnswerPolicy.RAG_MODEL.name(), result.getAnswerPolicy());
    }

    @Test
    void chatFallsBackWhenModelFails() {
        RagChatServiceImpl service = createService(true, List.of(mockKnowledge(0.9d)), null);
        ChatRequestDTO request = new ChatRequestDTO();
        request.setQuestion("\u8fd9\u6b3e\u5546\u54c1\u6709\u4ec0\u4e48\u5356\u70b9");
        request.setProductId(1001L);
        request.setContextType("product-detail");

        ChatResponseVO result = service.chat(10001L, request);

        assertEquals("MODEL_UNAVAILABLE", result.getFallbackReason());
        assertEquals(AnswerPolicy.RAG_FALLBACK_MODEL_ERROR.name(), result.getAnswerPolicy());
        assertTrue(result.getAnswer().contains("\u77e5\u8bc6\u5e93"));
    }

    @Test
    void chatReturnsFixedAssistantIntroForIdentityQuestion() {
        RagChatServiceImpl service = createService(false, List.of(mockKnowledge(0.9d)), null);
        ChatRequestDTO request = new ChatRequestDTO();
        request.setQuestion("\u4f60\u597d\uff0c\u4f60\u662f\u8c01");

        ChatResponseVO result = service.chat(10001L, request);

        assertEquals(QuestionCategory.OUT_OF_SCOPE.name(), result.getCategory());
        assertEquals("ASSISTANT_INTRO", result.getFallbackReason());
        assertEquals(AnswerPolicy.FIXED_TEMPLATE.name(), result.getAnswerPolicy());
        assertTrue(result.getAnswer().contains("\u5546\u57ce\u5546\u54c1\u77e5\u8bc6\u5ba2\u670d"));
    }

    @Test
    void chatAcceptsExplicitProductNameWithoutProductId() {
        RelatedKnowledgeVO knowledge = new RelatedKnowledgeVO();
        knowledge.setDocumentId("product-iphone15");
        knowledge.setTitle("iPhone 15");
        knowledge.setSourceType("PRODUCT");
        knowledge.setSourceId("2001");
        knowledge.setSnippet("\u8fd9\u662f\u4e00\u6b3e\u4e3b\u6253\u5f71\u50cf\u3001\u6027\u80fd\u4e0e\u65e5\u5e38\u901a\u8baf\u4f53\u9a8c\u7684\u624b\u673a\u3002");
        knowledge.setScore(0.91d);

        RagChatServiceImpl service = createService(false, List.of(knowledge), null);
        ChatRequestDTO request = new ChatRequestDTO();
        request.setQuestion("iPhone 15 \u662f\u505a\u4ec0\u4e48\u7684");

        ChatResponseVO result = service.chat(10001L, request);

        assertEquals(QuestionCategory.PRODUCT_INFO.name(), result.getCategory());
        assertEquals(AnswerPolicy.RAG_MODEL.name(), result.getAnswerPolicy());
    }

    @Test
    void chatRejectsAmbiguousThisProductQuestionWithoutContext() {
        RagChatServiceImpl service = createService(false, List.of(mockKnowledge(0.95d)), null);
        ChatRequestDTO request = new ChatRequestDTO();
        request.setQuestion("\u8fd9\u6b3e\u5546\u54c1\u662f\u505a\u4ec0\u4e48\u7684");

        ChatResponseVO result = service.chat(10001L, request);

        assertEquals(QuestionCategory.PRODUCT_INFO.name(), result.getCategory());
        assertEquals(AnswerPolicy.RAG_FALLBACK_NO_KNOWLEDGE.name(), result.getAnswerPolicy());
        assertTrue(result.getAnswer().contains("\u54ea\u4ef6\u5546\u54c1"));
    }

    private RagChatServiceImpl createService(boolean modelFails, List<RelatedKnowledgeVO> knowledge,
                                             ProductKnowledgeDTO product) {
        EmbeddingClient embeddingClient = Mockito.mock(EmbeddingClient.class);
        Mockito.when(embeddingClient.embed(any())).thenReturn(List.of(0.1, 0.2, 0.3));

        ChatModelClient chatModelClient = Mockito.mock(ChatModelClient.class);
        if (modelFails) {
            Mockito.when(chatModelClient.chat(any())).thenThrow(new ModelInvokeException("down"));
        } else {
            Mockito.when(chatModelClient.chat(any())).thenReturn("\u6a21\u578b\u56de\u7b54");
        }

        ProductKnowledgeClient productClient = Mockito.mock(ProductKnowledgeClient.class);
        Mockito.when(productClient.getProductById(eq(1001L))).thenReturn(product);

        SeckillKnowledgeClient seckillClient = Mockito.mock(SeckillKnowledgeClient.class);

        KnowledgeRetrievalService retrievalService = Mockito.mock(KnowledgeRetrievalService.class);
        Mockito.when(retrievalService.retrieve(any(), any(), any(), any())).thenReturn(knowledge);

        PromptBuilderService promptBuilderService = Mockito.mock(PromptBuilderService.class);
        Mockito.when(promptBuilderService.buildPrompt(any(), any(), any(), any(), any())).thenReturn("prompt");

        ChatSessionService chatSessionService = Mockito.mock(ChatSessionService.class);
        ChatSessionPO session = new ChatSessionPO();
        session.setSessionId("session-1");
        session.setProductId(1001L);
        session.setCreatedAt(LocalDateTime.now());
        session.setLastActiveAt(LocalDateTime.now());
        Mockito.when(chatSessionService.getOrCreate(any(), any(), any(), any())).thenReturn(session);

        ChatRecordService chatRecordService = Mockito.mock(ChatRecordService.class);
        ChatRecordPO historyRecord = new ChatRecordPO();
        historyRecord.setRecordNo(1);
        historyRecord.setQuestion("\u8001\u95ee\u9898");
        historyRecord.setAnswer("\u8001\u56de\u7b54");
        historyRecord.setCreatedAt(LocalDateTime.now().minusMinutes(1));
        Mockito.when(chatRecordService.listRecentHistory(any(), any(Integer.class))).thenReturn(List.of(historyRecord));
        Mockito.when(chatRecordService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ChatAuditService chatAuditService = Mockito.mock(ChatAuditService.class);
        Mockito.when(chatAuditService.buildAuditSummary(any(), any(), any(), any(), any())).thenReturn("audit-summary");

        ChatJsonCodec chatJsonCodec = new ChatJsonCodec(new ObjectMapper());
        InMemoryKnowledgeStore store = new InMemoryKnowledgeStore();
        KnowledgeChunkPO chunk = new KnowledgeChunkPO();
        chunk.setId("chunk-iphone15");
        chunk.setDocumentId("product-2001");
        chunk.setSourceType(KnowledgeSourceType.PRODUCT);
        chunk.setSourceId("2001");
        chunk.setTitle("iPhone 15");
        chunk.setContent("\u5546\u54c1\u540d\u79f0: iPhone 15");
        chunk.setEmbedding(List.of(0.1, 0.2, 0.3));
        store.replaceChunks(List.of(chunk));

        AiProperties properties = new AiProperties();
        properties.setMinConfidence(0.6d);
        properties.setHistoryLimit(5);
        properties.setHistoryCacheSize(5);
        properties.setSessionTtlDays(7);
        properties.setChatModel("qwen-turbo");

        return new RagChatServiceImpl(
                embeddingClient,
                chatModelClient,
                productClient,
                seckillClient,
                retrievalService,
                promptBuilderService,
                chatSessionService,
                chatRecordService,
                chatAuditService,
                chatJsonCodec,
                store,
                properties
        );
    }

    private RelatedKnowledgeVO mockKnowledge(double score) {
        RelatedKnowledgeVO knowledge = new RelatedKnowledgeVO();
        knowledge.setDocumentId("doc-1");
        knowledge.setTitle("\u5546\u54c1\u8be6\u60c5");
        knowledge.setSourceType("PRODUCT");
        knowledge.setSourceId("1001");
        knowledge.setSnippet("\u5546\u54c1\u652f\u6301 7 \u5929\u65e0\u7406\u7531\u9000\u8d27\u3002");
        knowledge.setScore(score);
        return knowledge;
    }
}
