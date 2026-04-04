package com.flashsale.aiservice.service;

import com.flashsale.aiservice.domain.dto.KnowledgeSyncRequestDTO;
import com.flashsale.aiservice.domain.vo.KnowledgeStatsVO;
import com.flashsale.aiservice.domain.vo.KnowledgeSyncResultVO;

public interface KnowledgeSyncService {

    KnowledgeSyncResultVO sync(KnowledgeSyncRequestDTO request);

    KnowledgeSyncResultVO getTask(String taskId);

    KnowledgeStatsVO getStats();
}
