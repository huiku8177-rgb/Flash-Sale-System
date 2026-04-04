package com.flashsale.aiservice.service.impl;

import com.flashsale.aiservice.domain.po.KnowledgeChunkPO;
import com.flashsale.aiservice.service.VectorStoreService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VectorStoreServiceImpl implements VectorStoreService {

    private final InMemoryKnowledgeStore knowledgeStore;

    public VectorStoreServiceImpl(InMemoryKnowledgeStore knowledgeStore) {
        this.knowledgeStore = knowledgeStore;
    }

    @Override
    public void replaceAll(List<KnowledgeChunkPO> chunks) {
        knowledgeStore.replaceChunks(chunks);
    }

    @Override
    public List<KnowledgeChunkPO> allChunks() {
        return knowledgeStore.getAllChunks();
    }
}
