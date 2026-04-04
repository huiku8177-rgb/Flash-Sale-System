package com.flashsale.aiservice.service.impl;

import com.flashsale.aiservice.client.EmbeddingClient;
import com.flashsale.aiservice.domain.po.KnowledgeChunkPO;
import com.flashsale.aiservice.domain.po.KnowledgeDocumentPO;
import com.flashsale.aiservice.service.DocumentChunkService;
import com.flashsale.aiservice.util.TextChunkUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentChunkServiceImpl implements DocumentChunkService {

    private final EmbeddingClient embeddingClient;

    @Override
    public List<KnowledgeChunkPO> chunk(KnowledgeDocumentPO document) {
        List<String> texts = TextChunkUtils.chunk(document.getContent());
        List<KnowledgeChunkPO> chunks = new ArrayList<>(texts.size());
        for (int index = 0; index < texts.size(); index++) {
            KnowledgeChunkPO chunk = new KnowledgeChunkPO();
            chunk.setId(document.getId() + "-chunk-" + index);
            chunk.setDocumentId(document.getId());
            chunk.setSourceType(document.getSourceType());
            chunk.setSourceId(document.getSourceId());
            chunk.setTitle(document.getTitle());
            chunk.setContent(texts.get(index));
            chunk.setEmbedding(embeddingClient.embed(texts.get(index)));
            chunks.add(chunk);
        }
        return chunks;
    }
}
