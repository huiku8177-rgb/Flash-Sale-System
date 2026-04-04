package com.flashsale.aiservice.service;

import com.flashsale.aiservice.domain.po.KnowledgeChunkPO;
import com.flashsale.aiservice.domain.po.KnowledgeDocumentPO;

import java.util.List;

public interface DocumentChunkService {

    List<KnowledgeChunkPO> chunk(KnowledgeDocumentPO document);
}
