package com.flashsale.aiservice.service;

import com.flashsale.aiservice.domain.po.KnowledgeChunkPO;

import java.util.List;

public interface VectorStoreService {

    void replaceAll(List<KnowledgeChunkPO> chunks);

    List<KnowledgeChunkPO> allChunks();
}
