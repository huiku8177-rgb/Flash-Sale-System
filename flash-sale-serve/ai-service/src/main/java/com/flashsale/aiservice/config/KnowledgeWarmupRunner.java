package com.flashsale.aiservice.config;

import com.flashsale.aiservice.domain.dto.KnowledgeSyncRequestDTO;
import com.flashsale.aiservice.service.KnowledgeSyncService;
import com.flashsale.aiservice.service.impl.InMemoryKnowledgeStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeWarmupRunner implements ApplicationRunner {

    private final KnowledgeSyncService knowledgeSyncService;
    private final AiProperties aiProperties;
    private final InMemoryKnowledgeStore knowledgeStore;

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("Starting initial knowledge sync, productServiceUrl={}, seckillServiceUrl={}",
                    aiProperties.getProductServiceUrl(), aiProperties.getSeckillServiceUrl());
            knowledgeSyncService.sync(new KnowledgeSyncRequestDTO());
            log.info("Initial knowledge sync completed");
        } catch (Exception ex) {
            knowledgeStore.markSyncFailed("Initial knowledge warmup failed: " + ex.getMessage());
            log.warn("Initial knowledge sync skipped, productServiceUrl={}, seckillServiceUrl={}, reason={}",
                    aiProperties.getProductServiceUrl(), aiProperties.getSeckillServiceUrl(), ex.getMessage(), ex);
        }
    }
}
