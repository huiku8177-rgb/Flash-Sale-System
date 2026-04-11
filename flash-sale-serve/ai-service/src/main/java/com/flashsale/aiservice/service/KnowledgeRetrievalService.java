package com.flashsale.aiservice.service;

import com.flashsale.aiservice.domain.vo.RelatedKnowledgeVO;
import com.flashsale.aiservice.service.model.KnowledgeRetrieveRequest;

import java.util.List;

public interface KnowledgeRetrievalService {

    List<RelatedKnowledgeVO> retrieve(KnowledgeRetrieveRequest request);
}
