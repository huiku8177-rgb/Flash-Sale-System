package com.flashsale.aiservice.service.impl;

import com.flashsale.aiservice.client.ChatModelClient;
import com.flashsale.aiservice.client.EmbeddingClient;
import com.flashsale.aiservice.client.ProductKnowledgeClient;
import com.flashsale.aiservice.domain.dto.ChatRequestDTO;
import com.flashsale.aiservice.domain.dto.ProductKnowledgeDTO;
import com.flashsale.aiservice.domain.vo.ChatResponseVO;
import com.flashsale.aiservice.service.RagChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagChatServiceImpl implements RagChatService {

    private final EmbeddingClient embeddingClient;
    private final ChatModelClient chatModelClient;
    private final ProductKnowledgeClient productKnowledgeClient;

    @Override
    public ChatResponseVO chat(ChatRequestDTO request) {
        String question = request.getQuestion();

        List<Double> questionEmbedding = embeddingClient.embed(question);
        log.info("Question embedded, dimensions={}", questionEmbedding.size());

        List<String> knowledgeSnippets = retrieveKnowledge(questionEmbedding);

        String prompt = buildPrompt(question, knowledgeSnippets);

        String answer = chatModelClient.chat(prompt);
        log.info("Chat answer received, length={}", answer.length());

        ChatResponseVO vo = new ChatResponseVO();
        vo.setAnswer(answer);
        vo.setSources(knowledgeSnippets.isEmpty()
                ? List.of("暂无知识库参考") : knowledgeSnippets);
        return vo;
    }

    private List<String> retrieveKnowledge(List<Double> questionEmbedding) {
        log.debug("Starting minimal knowledge retrieval");

        List<ProductKnowledgeDTO> products = productKnowledgeClient.getAllProducts();
        log.debug("Retrieved {} products from product-service", products.size());

        if (products.isEmpty()) {
            log.warn("No products available for knowledge retrieval");
            return List.of();
        }

        List<String> knowledgeTexts = products.stream()
                .limit(5)
                .map(ProductKnowledgeDTO::toKnowledgeText)
                .toList();

        log.debug("Generated {} knowledge snippets", knowledgeTexts.size());
        return knowledgeTexts;
    }

    private String buildPrompt(String question, List<String> knowledgeSnippets) {
        if (knowledgeSnippets.isEmpty()) {
            return "你是一个电商客服助手。请根据你的知识回答用户问题。\n\n用户问题：" + question;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("你是一个电商客服助手。请根据以下商品知识回答用户问题，如果知识中没有相关内容请如实告知。\n\n");
        sb.append("【商品知识】\n");
        for (String snippet : knowledgeSnippets) {
            sb.append("- ").append(snippet).append("\n");
        }
        sb.append("\n用户问题：").append(question);
        return sb.toString();
    }
}
