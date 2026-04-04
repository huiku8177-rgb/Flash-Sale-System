package com.flashsale.aiservice.service.impl;

import com.flashsale.aiservice.client.ChatModelClient;
import com.flashsale.aiservice.client.EmbeddingClient;
import com.flashsale.aiservice.client.ProductKnowledgeClient;
import com.flashsale.aiservice.client.SeckillKnowledgeClient;
import com.flashsale.aiservice.config.AiProperties;
import com.flashsale.aiservice.domain.dto.ChatRequestDTO;
import com.flashsale.aiservice.domain.dto.ProductKnowledgeDTO;
import com.flashsale.aiservice.domain.dto.SeckillKnowledgeDTO;
import com.flashsale.aiservice.domain.enums.AnswerPolicy;
import com.flashsale.aiservice.domain.enums.QuestionCategory;
import com.flashsale.aiservice.domain.po.ChatRecordPO;
import com.flashsale.aiservice.domain.po.ChatSessionPO;
import com.flashsale.aiservice.domain.vo.ChatResponseVO;
import com.flashsale.aiservice.domain.vo.ChatSessionVO;
import com.flashsale.aiservice.domain.vo.RelatedKnowledgeVO;
import com.flashsale.aiservice.exception.ModelInvokeException;
import com.flashsale.aiservice.service.ChatAuditService;
import com.flashsale.aiservice.service.ChatRecordService;
import com.flashsale.aiservice.service.ChatSessionService;
import com.flashsale.aiservice.service.KnowledgeRetrievalService;
import com.flashsale.aiservice.service.PromptBuilderService;
import com.flashsale.aiservice.service.RagChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagChatServiceImpl implements RagChatService {

    private static final String FALLBACK_ASSISTANT_INTRO = "ASSISTANT_INTRO";
    private static final String FALLBACK_GREETING = "GREETING";
    private static final String FALLBACK_OUT_OF_SCOPE = "OUT_OF_SCOPE";
    private static final String FALLBACK_NO_RELEVANT_KNOWLEDGE = "NO_RELEVANT_KNOWLEDGE";
    private static final String FALLBACK_MODEL_UNAVAILABLE = "MODEL_UNAVAILABLE";

    private static final String QUESTION_WEATHER = "\u5929\u6c14";
    private static final String QUESTION_STOCK = "\u80a1\u7968";
    private static final String QUESTION_CODE = "\u4ee3\u7801";
    private static final String QUESTION_PRESIDENT = "\u603b\u7edf";

    private static final String TERM_REFUND = "\u9000\u6b3e";
    private static final String TERM_RETURN = "\u9000\u8d27";
    private static final String TERM_AFTER_SALES = "\u552e\u540e";
    private static final String TERM_WARRANTY = "\u4fdd\u4fee";

    private static final String TERM_DELIVERY = "\u914d\u9001";
    private static final String TERM_SHIPPING = "\u53d1\u8d27";
    private static final String TERM_EXPRESS = "\u5feb\u9012";
    private static final String TERM_LOGISTICS = "\u7269\u6d41";
    private static final String TERM_FREIGHT = "\u8fd0\u8d39";

    private static final String TERM_INVENTORY = "\u5e93\u5b58";
    private static final String TERM_AVAILABLE = "\u6709\u8d27";
    private static final String TERM_PRICE = "\u4ef7\u683c";
    private static final String TERM_HOW_MUCH = "\u591a\u5c11\u94b1";
    private static final String TERM_SECKILL_PRICE = "\u79d2\u6740\u4ef7";

    private static final String TERM_SECKILL = "\u79d2\u6740";
    private static final String TERM_ACTIVITY = "\u6d3b\u52a8";
    private static final String TERM_START = "\u5f00\u59cb";
    private static final String TERM_END = "\u7ed3\u675f";

    private static final String TERM_PRODUCT = "\u5546\u54c1";
    private static final String TERM_THIS_PRODUCT = "\u8fd9\u6b3e";
    private static final String TERM_THIS_ITEM = "\u8fd9\u4e2a";
    private static final String TERM_THIS_GOODS = "\u8fd9\u4e2a\u5546\u54c1";
    private static final String TERM_THAT_PRODUCT = "\u8be5\u5546\u54c1";
    private static final String TERM_INTRO = "\u4ecb\u7ecd";
    private static final String TERM_DETAIL = "\u8be6\u60c5";
    private static final String TERM_SELLING_POINT = "\u5356\u70b9";
    private static final String TERM_SPEC = "\u89c4\u683c";
    private static final String TERM_MODEL = "\u578b\u53f7";
    private static final String TERM_SIZE = "\u5c3a\u5bf8";
    private static final String TERM_COLOR = "\u989c\u8272";
    private static final String TERM_PARAM = "\u53c2\u6570";

    private static final String TERM_WHO_ARE_YOU = "\u4f60\u662f\u8c01";
    private static final String TERM_WHAT_IS_YOUR_NAME = "\u4f60\u53eb\u4ec0\u4e48";
    private static final String TERM_WHAT_CAN_YOU_DO = "\u4f60\u80fd\u505a\u4ec0\u4e48";
    private static final String TERM_INTRODUCE_YOURSELF = "\u4ecb\u7ecd\u4e00\u4e0b\u4f60\u81ea\u5df1";
    private static final String TERM_HELLO = "\u4f60\u597d";
    private static final String TERM_HELLO_POLITE = "\u60a8\u597d";
    private static final String TERM_ARE_YOU_THERE = "\u5728\u5417";

    private final EmbeddingClient embeddingClient;
    private final ChatModelClient chatModelClient;
    private final ProductKnowledgeClient productKnowledgeClient;
    private final SeckillKnowledgeClient seckillKnowledgeClient;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final PromptBuilderService promptBuilderService;
    private final ChatSessionService chatSessionService;
    private final ChatRecordService chatRecordService;
    private final ChatAuditService chatAuditService;
    private final ChatJsonCodec chatJsonCodec;
    private final InMemoryKnowledgeStore knowledgeStore;
    private final AiProperties aiProperties;

    @Override
    @Transactional
    public ChatResponseVO chat(Long userId, ChatRequestDTO request) {
        long startNanos = System.nanoTime();

        ChatSessionPO session = chatSessionService.getOrCreate(
                request.getSessionId(),
                userId,
                request.getProductId(),
                request.getContextType()
        );
        Long effectiveProductId = request.getProductId() != null ? request.getProductId() : session.getProductId();

        QuestionIntent intent = detectIntent(request.getQuestion(), request.getContextType(), effectiveProductId);
        QuestionCategory category = classifyCategory(request.getQuestion(), request.getContextType(), intent);

        knowledgeStore.incrementChatRequests();

        List<ChatRecordPO> history = chatRecordService.listRecentHistory(
                session.getSessionId(),
                aiProperties.getHistoryLimit()
        );
        ChatResponseVO response = buildChatResponse(request, session, effectiveProductId, intent, category, history);

        long latencyMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
        long estimatedTokens = estimateTokens(request.getQuestion(), response.getAnswer());
        persistChatResult(userId, request, session, effectiveProductId, category, response, latencyMs, estimatedTokens);

        if (!response.getHitKnowledge().isEmpty()) {
            knowledgeStore.incrementHitRequests();
        }
        if (StringUtils.hasText(response.getFallbackReason())) {
            knowledgeStore.incrementFallbacks();
        }
        knowledgeStore.recordLatency(latencyMs, estimatedTokens);
        return response;
    }

    @Override
    public ChatSessionVO getSession(Long userId, String sessionId) {
        ChatSessionPO session = chatSessionService.getRequired(sessionId, userId);
        return chatRecordService.getSessionDetail(session, aiProperties.getSessionQueryLimit());
    }

    private ChatResponseVO buildChatResponse(ChatRequestDTO request, ChatSessionPO session, Long productId,
                                             QuestionIntent intent, QuestionCategory category, List<ChatRecordPO> history) {
        if (intent == QuestionIntent.IDENTITY) {
            return buildResponse(
                    session.getSessionId(),
                    QuestionCategory.OUT_OF_SCOPE,
                    "\u6211\u662f\u5546\u57ce\u5546\u54c1\u77e5\u8bc6\u5ba2\u670d\u52a9\u624b\uff0c\u4e3b\u8981\u7528\u4e8e\u56de\u7b54\u5546\u54c1\u4fe1\u606f\u3001\u79d2\u6740\u6d3b\u52a8\u3001\u914d\u9001\u548c\u552e\u540e\u76f8\u5173\u95ee\u9898\u3002",
                    List.of(),
                    1d,
                    FALLBACK_ASSISTANT_INTRO,
                    AnswerPolicy.FIXED_TEMPLATE
            );
        }
        if (intent == QuestionIntent.GREETING) {
            return buildResponse(
                    session.getSessionId(),
                    QuestionCategory.OUT_OF_SCOPE,
                    "\u4f60\u597d\uff0c\u6211\u53ef\u4ee5\u5e2e\u4f60\u89e3\u7b54\u5546\u54c1\u4fe1\u606f\u3001\u6d3b\u52a8\u89c4\u5219\u3001\u5e93\u5b58\u4ef7\u683c\u3001\u914d\u9001\u548c\u552e\u540e\u95ee\u9898\u3002",
                    List.of(),
                    1d,
                    FALLBACK_GREETING,
                    AnswerPolicy.FIXED_TEMPLATE
            );
        }
        if (intent == QuestionIntent.OUT_OF_SCOPE) {
            return buildResponse(
                    session.getSessionId(),
                    QuestionCategory.OUT_OF_SCOPE,
                    "\u5f53\u524d\u4ec5\u652f\u6301\u5546\u54c1\u4fe1\u606f\u3001\u6d3b\u52a8\u89c4\u5219\u3001\u914d\u9001\u548c\u552e\u540e\u76f8\u5173\u54a8\u8be2\uff0c\u8bf7\u63cf\u8ff0\u5177\u4f53\u5546\u54c1\u95ee\u9898\u6216\u8054\u7cfb\u4eba\u5de5\u5ba2\u670d\u3002",
                    List.of(),
                    0d,
                    FALLBACK_OUT_OF_SCOPE,
                    AnswerPolicy.OUT_OF_SCOPE_REFUSAL
            );
        }

        List<Double> questionEmbedding = embeddingClient.embed(request.getQuestion());
        List<RelatedKnowledgeVO> hitKnowledge = new ArrayList<>(
                knowledgeRetrievalService.retrieve(request.getQuestion(), questionEmbedding, category, productId)
        );

        String realtimeFacts = buildRealtimeFacts(productId, category);
        if (StringUtils.hasText(realtimeFacts)) {
            hitKnowledge.add(realtimeKnowledge(productId, realtimeFacts));
        }

        double confidence = hitKnowledge.stream().mapToDouble(RelatedKnowledgeVO::getScore).max().orElse(0d);
        if (!hasReliableEvidence(request, category, productId, hitKnowledge, confidence)) {
            knowledgeStore.incrementNoResult();
            return buildResponse(
                    session.getSessionId(),
                    category,
                    buildNoKnowledgeAnswer(request.getQuestion(), category, productId, hitKnowledge, realtimeFacts),
                    hitKnowledge,
                    confidence,
                    FALLBACK_NO_RELEVANT_KNOWLEDGE,
                    StringUtils.hasText(realtimeFacts) ? AnswerPolicy.REALTIME_ONLY : AnswerPolicy.RAG_FALLBACK_NO_KNOWLEDGE
            );
        }

        String prompt = promptBuilderService.buildPrompt(
                request.getQuestion(),
                category,
                hitKnowledge,
                history,
                realtimeFacts
        );
        try {
            return buildResponse(
                    session.getSessionId(),
                    category,
                    chatModelClient.chat(prompt),
                    hitKnowledge,
                    confidence,
                    null,
                    AnswerPolicy.RAG_MODEL
            );
        } catch (ModelInvokeException ex) {
            knowledgeStore.incrementModelFailures();
            log.warn("Chat model unavailable, fallback to deterministic answer: {}", ex.getMessage());
            return buildResponse(
                    session.getSessionId(),
                    category,
                    safeDeterministicAnswer(hitKnowledge, realtimeFacts, category),
                    hitKnowledge,
                    confidence,
                    FALLBACK_MODEL_UNAVAILABLE,
                    StringUtils.hasText(realtimeFacts) ? AnswerPolicy.REALTIME_ONLY : AnswerPolicy.RAG_FALLBACK_MODEL_ERROR
            );
        }
    }

    private void persistChatResult(Long userId, ChatRequestDTO request, ChatSessionPO session, Long productId,
                                   QuestionCategory category, ChatResponseVO response, long latencyMs, long estimatedTokens) {
        String auditSummary = chatAuditService.buildAuditSummary(
                request.getQuestion(),
                response.getAnswer(),
                AnswerPolicy.valueOf(response.getAnswerPolicy()),
                response.getFallbackReason(),
                response.getHitKnowledge()
        );

        ChatRecordPO record = new ChatRecordPO();
        record.setSessionId(session.getSessionId());
        record.setUserId(userId);
        record.setProductId(productId);
        record.setQuestion(request.getQuestion());
        record.setQuestionCategory(category.name());
        record.setAnswer(response.getAnswer());
        record.setAnswerPolicy(response.getAnswerPolicy());
        record.setSourcesJson(chatJsonCodec.writeStringList(response.getSources()));
        record.setHitKnowledgeJson(chatJsonCodec.writeKnowledgeList(response.getHitKnowledge()));
        record.setConfidence(BigDecimal.valueOf(response.getConfidence()));
        record.setFallbackReason(response.getFallbackReason());
        record.setAuditSummary(auditSummary);
        record.setModelName(aiProperties.getChatModel());
        record.setLatencyMs((int) latencyMs);
        record.setEstimatedTokens((int) estimatedTokens);
        record.setCreatedAt(LocalDateTime.now());
        record.setExpireAt(record.getCreatedAt().plusDays(aiProperties.getSessionTtlDays()));
        chatRecordService.save(record);

        chatSessionService.refreshSession(
                session,
                request.getQuestion(),
                summarizeAnswer(response.getAnswer()),
                productId,
                request.getContextType()
        );
    }

    private QuestionIntent detectIntent(String question, String contextType, Long productId) {
        String normalizedQuestion = normalize(question);
        if (isAssistantIdentityQuestion(normalizedQuestion)) {
            return QuestionIntent.IDENTITY;
        }
        if (isGreetingOnly(normalizedQuestion)) {
            return QuestionIntent.GREETING;
        }
        if (containsAny(normalizedQuestion, QUESTION_WEATHER, QUESTION_STOCK, QUESTION_CODE, QUESTION_PRESIDENT)) {
            return QuestionIntent.OUT_OF_SCOPE;
        }
        if (hasBusinessSignals(normalizedQuestion)
                || hasProductContext(contextType, productId)
                || mentionsKnownProduct(normalizedQuestion)) {
            return QuestionIntent.DOMAIN_QA;
        }
        return QuestionIntent.OUT_OF_SCOPE;
    }

    private QuestionCategory classifyCategory(String question, String contextType, QuestionIntent intent) {
        if (intent != QuestionIntent.DOMAIN_QA) {
            return QuestionCategory.OUT_OF_SCOPE;
        }

        String normalizedQuestion = normalize(question);
        if (containsAny(normalizedQuestion, TERM_REFUND, TERM_RETURN, TERM_AFTER_SALES, TERM_WARRANTY)) {
            return QuestionCategory.AFTER_SALES_POLICY;
        }
        if (containsAny(normalizedQuestion, TERM_DELIVERY, TERM_SHIPPING, TERM_EXPRESS, TERM_LOGISTICS, TERM_FREIGHT)) {
            return QuestionCategory.DELIVERY_POLICY;
        }
        if (containsAny(normalizedQuestion, TERM_INVENTORY, TERM_AVAILABLE, TERM_PRICE, TERM_HOW_MUCH, TERM_SECKILL_PRICE)) {
            return QuestionCategory.REALTIME_STATUS;
        }
        if (containsAny(normalizedQuestion, TERM_SECKILL, TERM_ACTIVITY, TERM_START, TERM_END)) {
            return QuestionCategory.ACTIVITY_RULE;
        }
        if (contextType != null && contextType.toLowerCase(Locale.ROOT).contains("product")) {
            return QuestionCategory.PRODUCT_INFO;
        }
        return QuestionCategory.PRODUCT_INFO;
    }

    private boolean hasReliableEvidence(ChatRequestDTO request, QuestionCategory category, Long productId,
                                        List<RelatedKnowledgeVO> hitKnowledge, double confidence) {
        if (hitKnowledge.isEmpty()) {
            return false;
        }
        if (category == QuestionCategory.PRODUCT_INFO
                && isAmbiguousProductReference(request.getQuestion())
                && !hasProductContext(request.getContextType(), productId)) {
            return false;
        }
        if (category == QuestionCategory.REALTIME_STATUS || category == QuestionCategory.ACTIVITY_RULE) {
            if (hitKnowledge.stream().anyMatch(RelatedKnowledgeVO::isRealtime)) {
                return true;
            }
            if (confidence < aiProperties.getMinConfidence()) {
                return false;
            }
        }
        if (confidence < aiProperties.getMinConfidence()) {
            return false;
        }

        RelatedKnowledgeVO topKnowledge = hitKnowledge.stream()
                .filter(item -> !item.isRealtime())
                .findFirst()
                .orElse(hitKnowledge.get(0));

        return switch (category) {
            case AFTER_SALES_POLICY, DELIVERY_POLICY -> "RULE".equalsIgnoreCase(topKnowledge.getSourceType());
            case ACTIVITY_RULE -> "RULE".equalsIgnoreCase(topKnowledge.getSourceType())
                    || "SECKILL".equalsIgnoreCase(topKnowledge.getSourceType())
                    || topKnowledge.isRealtime();
            case PRODUCT_INFO -> hasProductContext(request.getContextType(), productId)
                    || matchesExplicitProductMention(request.getQuestion(), topKnowledge);
            case REALTIME_STATUS -> topKnowledge.isRealtime()
                    || hasProductContext(request.getContextType(), productId)
                    || matchesExplicitProductMention(request.getQuestion(), topKnowledge);
            default -> false;
        };
    }

    private boolean hasBusinessSignals(String normalizedQuestion) {
        return containsAny(
                normalizedQuestion,
                TERM_PRODUCT,
                TERM_THIS_PRODUCT,
                TERM_THIS_ITEM,
                TERM_THIS_GOODS,
                TERM_THAT_PRODUCT,
                TERM_INTRO,
                TERM_DETAIL,
                TERM_SELLING_POINT,
                TERM_SPEC,
                TERM_MODEL,
                TERM_SIZE,
                TERM_COLOR,
                TERM_PARAM,
                TERM_REFUND,
                TERM_RETURN,
                TERM_AFTER_SALES,
                TERM_WARRANTY,
                TERM_DELIVERY,
                TERM_SHIPPING,
                TERM_EXPRESS,
                TERM_LOGISTICS,
                TERM_FREIGHT,
                TERM_INVENTORY,
                TERM_AVAILABLE,
                TERM_PRICE,
                TERM_HOW_MUCH,
                TERM_SECKILL_PRICE,
                TERM_SECKILL,
                TERM_ACTIVITY,
                TERM_START,
                TERM_END
        );
    }

    private boolean hasProductContext(String contextType, Long productId) {
        return productId != null || (contextType != null && contextType.toLowerCase(Locale.ROOT).contains("product"));
    }

    private boolean isAssistantIdentityQuestion(String normalizedQuestion) {
        return containsAny(
                normalizedQuestion,
                TERM_WHO_ARE_YOU,
                TERM_WHAT_IS_YOUR_NAME,
                TERM_WHAT_CAN_YOU_DO,
                TERM_INTRODUCE_YOURSELF
        );
    }

    private boolean isGreetingOnly(String normalizedQuestion) {
        String compactQuestion = normalizedQuestion
                .replace("\uFF0C", "")
                .replace("\u3002", "")
                .replace("\uFF01", "")
                .replace("\uFF1F", "")
                .replace(",", "")
                .replace(".", "")
                .replace("!", "")
                .replace("?", "")
                .trim();
        return compactQuestion.equals(TERM_HELLO)
                || compactQuestion.equals(TERM_HELLO_POLITE)
                || compactQuestion.equals("hi")
                || compactQuestion.equals("hello")
                || compactQuestion.equals(TERM_ARE_YOU_THERE);
    }

    private boolean mentionsKnownProduct(String normalizedQuestion) {
        if (!StringUtils.hasText(normalizedQuestion)) {
            return false;
        }
        return knowledgeStore.getAllChunks().stream()
                .filter(chunk -> chunk.getSourceType() != null)
                .filter(chunk -> "PRODUCT".equalsIgnoreCase(chunk.getSourceType().name())
                        || "SECKILL".equalsIgnoreCase(chunk.getSourceType().name()))
                .map(chunk -> compact(chunk.getTitle()))
                .filter(StringUtils::hasText)
                .anyMatch(compactTitle -> compact(normalizedQuestion).contains(compactTitle));
    }

    private boolean matchesExplicitProductMention(String question, RelatedKnowledgeVO knowledge) {
        if (knowledge == null || !StringUtils.hasText(knowledge.getTitle())) {
            return false;
        }
        if (!"PRODUCT".equalsIgnoreCase(knowledge.getSourceType())
                && !"SECKILL".equalsIgnoreCase(knowledge.getSourceType())) {
            return false;
        }
        String compactQuestion = compact(question);
        String compactTitle = compact(knowledge.getTitle());
        return StringUtils.hasText(compactQuestion)
                && StringUtils.hasText(compactTitle)
                && compactQuestion.contains(compactTitle);
    }

    private boolean isAmbiguousProductReference(String question) {
        String normalizedQuestion = normalize(question);
        return containsAny(normalizedQuestion, TERM_THIS_PRODUCT, TERM_THIS_ITEM, TERM_THIS_GOODS, TERM_THAT_PRODUCT)
                && !mentionsKnownProduct(normalizedQuestion);
    }

    private String buildRealtimeFacts(Long productId, QuestionCategory category) {
        if ((category != QuestionCategory.REALTIME_STATUS && category != QuestionCategory.ACTIVITY_RULE) || productId == null) {
            return "";
        }

        ProductKnowledgeDTO product = productKnowledgeClient.getProductById(productId);
        if (product != null) {
            String status = product.getStatus() != null && product.getStatus() == 1
                    ? "\u4e0a\u67b6\u4e2d"
                    : "\u5df2\u4e0b\u67b6";
            return "\u5b9e\u65f6\u5546\u54c1\u4fe1\u606f\uff1a\u552e\u4ef7 " + product.getPrice()
                    + " \u5143\uff0c\u5e93\u5b58 " + product.getStock()
                    + " \u4ef6\uff0c\u72b6\u6001 " + status + "\u3002";
        }

        SeckillKnowledgeDTO seckill = seckillKnowledgeClient.getProductById(productId);
        if (seckill != null) {
            return "\u5b9e\u65f6\u79d2\u6740\u4fe1\u606f\uff1a\u79d2\u6740\u4ef7 " + seckill.getSeckillPrice()
                    + " \u5143\uff0c\u5e93\u5b58 " + seckill.getStock()
                    + " \u4ef6\uff0c\u6d3b\u52a8\u65f6\u95f4 " + seckill.getStartTime()
                    + " \u81f3 " + seckill.getEndTime() + "\u3002";
        }

        return "";
    }

    private RelatedKnowledgeVO realtimeKnowledge(Long productId, String realtimeFacts) {
        RelatedKnowledgeVO knowledge = new RelatedKnowledgeVO();
        knowledge.setDocumentId("realtime-" + (productId == null ? "unknown" : productId));
        knowledge.setTitle("\u5b9e\u65f6\u5546\u54c1\u4fe1\u606f");
        knowledge.setSourceType("REALTIME");
        knowledge.setSourceId(productId == null ? "" : String.valueOf(productId));
        knowledge.setSnippet(realtimeFacts);
        knowledge.setScore(1d);
        knowledge.setRealtime(true);
        return knowledge;
    }

    private ChatResponseVO buildResponse(String sessionId, QuestionCategory category, String answer,
                                         List<RelatedKnowledgeVO> hitKnowledge, double confidence,
                                         String fallbackReason, AnswerPolicy answerPolicy) {
        ChatResponseVO response = new ChatResponseVO();
        response.setSessionId(sessionId);
        response.setCategory(category.name());
        response.setAnswer(answer);
        response.setSources(hitKnowledge.stream().map(RelatedKnowledgeVO::getTitle).distinct().toList());
        response.setHitKnowledge(hitKnowledge);
        response.setConfidence(confidence);
        response.setFallbackReason(fallbackReason);
        response.setAnswerPolicy(answerPolicy.name());
        return response;
    }

    private String fallbackAnswer(QuestionCategory category, String realtimeFacts) {
        if (StringUtils.hasText(realtimeFacts)) {
            return realtimeFacts + "\u5982\u9700\u66f4\u51c6\u786e\u7ed3\u679c\uff0c\u8bf7\u4ee5\u9875\u9762\u5b9e\u65f6\u5c55\u793a\u6216\u4eba\u5de5\u5ba2\u670d\u786e\u8ba4\u4e3a\u51c6\u3002";
        }
        return switch (category) {
            case AFTER_SALES_POLICY -> "\u76ee\u524d\u6ca1\u6709\u8db3\u591f\u4f9d\u636e\u786e\u8ba4\u5177\u4f53\u552e\u540e\u7ec6\u8282\uff0c\u5efa\u8bae\u4ee5\u5546\u54c1\u8be6\u60c5\u9875\u552e\u540e\u8bf4\u660e\u6216\u4eba\u5de5\u5ba2\u670d\u7b54\u590d\u4e3a\u51c6\u3002";
            case DELIVERY_POLICY -> "\u76ee\u524d\u6ca1\u6709\u8db3\u591f\u4f9d\u636e\u786e\u8ba4\u914d\u9001\u65f6\u6548\u548c\u8fd0\u8d39\uff0c\u8bf7\u4ee5\u7ed3\u7b97\u9875\u548c\u5546\u54c1\u8be6\u60c5\u9875\u5c55\u793a\u4e3a\u51c6\u3002";
            case ACTIVITY_RULE -> "\u5f53\u524d\u65e0\u6cd5\u786e\u8ba4\u6d3b\u52a8\u89c4\u5219\u6216\u6d3b\u52a8\u65f6\u95f4\uff0c\u8bf7\u4ee5\u9875\u9762\u5b9e\u65f6\u6d3b\u52a8\u8bf4\u660e\u4e3a\u51c6\u3002";
            case REALTIME_STATUS -> "\u5f53\u524d\u65e0\u6cd5\u786e\u8ba4\u5b9e\u65f6\u5e93\u5b58\u3001\u4ef7\u683c\u6216\u6d3b\u52a8\u72b6\u6001\uff0c\u8bf7\u4ee5\u9875\u9762\u5b9e\u65f6\u5c55\u793a\u4e3a\u51c6\u3002";
            default -> "\u5f53\u524d\u6ca1\u6709\u68c0\u7d22\u5230\u8db3\u591f\u7684\u5546\u54c1\u77e5\u8bc6\uff0c\u5efa\u8bae\u8865\u5145\u5546\u54c1\u540d\u79f0\u3001\u578b\u53f7\u6216\u76f4\u63a5\u54a8\u8be2\u4eba\u5de5\u5ba2\u670d\u3002";
        };
    }

    private String buildNoKnowledgeAnswer(String question, QuestionCategory category, Long productId,
                                          List<RelatedKnowledgeVO> hitKnowledge, String realtimeFacts) {
        if (category == QuestionCategory.PRODUCT_INFO
                && isAmbiguousProductReference(question)
                && productId == null) {
            return "\u8fd8\u4e0d\u786e\u5b9a\u4f60\u6307\u7684\u662f\u54ea\u4ef6\u5546\u54c1\uff0c\u8bf7\u76f4\u63a5\u63d0\u4f9b\u5546\u54c1\u540d\u79f0\u3001\u578b\u53f7\uff0c\u6216\u4ece\u5546\u54c1\u8be6\u60c5\u9875\u8fdb\u5165 AI \u52a9\u624b\u63d0\u95ee\u3002";
        }
        if (category == QuestionCategory.PRODUCT_INFO
                && !StringUtils.hasText(realtimeFacts)
                && hitKnowledge.stream().noneMatch(item -> matchesExplicitProductMention(question, item))) {
            return "\u5f53\u524d\u6ca1\u6709\u68c0\u7d22\u5230\u4e0e\u8be5\u5546\u54c1\u540d\u79f0\u76f8\u5339\u914d\u7684\u77e5\u8bc6\uff0c\u8bf7\u68c0\u67e5\u5546\u54c1\u540d\u79f0\u662f\u5426\u6b63\u786e\uff0c\u6216\u76f4\u63a5\u5728\u5546\u54c1\u8be6\u60c5\u9875\u63d0\u95ee\u3002";
        }
        return fallbackAnswer(category, realtimeFacts);
    }

    private String safeDeterministicAnswer(List<RelatedKnowledgeVO> hitKnowledge, String realtimeFacts, QuestionCategory category) {
        if (StringUtils.hasText(realtimeFacts)) {
            return realtimeFacts + "\u5176\u4f59\u8bf4\u660e\u53ef\u53c2\u8003\u76f8\u5173\u5546\u54c1\u9875\u9762\u6216\u4eba\u5de5\u5ba2\u670d\u3002";
        }

        RelatedKnowledgeVO topKnowledge = hitKnowledge.stream()
                .filter(item -> !item.isRealtime())
                .findFirst()
                .orElse(null);
        if (topKnowledge == null) {
            return fallbackAnswer(category, realtimeFacts);
        }
        return "\u6839\u636e\u5f53\u524d\u77e5\u8bc6\u5e93\u4fe1\u606f\uff0c" + topKnowledge.getSnippet();
    }

    private String normalize(String question) {
        return question == null ? "" : question.toLowerCase(Locale.ROOT).trim();
    }

    private String compact(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\u4e00-\\u9fa5]+", "");
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String summarizeAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        return answer.length() <= 120 ? answer : answer.substring(0, 120);
    }

    private long estimateTokens(String question, String answer) {
        int length = (question == null ? 0 : question.length()) + (answer == null ? 0 : answer.length());
        return Math.max(1L, Math.round(length / 4.0));
    }

    private enum QuestionIntent {
        IDENTITY,
        GREETING,
        DOMAIN_QA,
        OUT_OF_SCOPE
    }
}
