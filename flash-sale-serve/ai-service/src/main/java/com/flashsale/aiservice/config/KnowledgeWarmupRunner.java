package com.flashsale.aiservice.config;

import com.flashsale.aiservice.domain.dto.KnowledgeSyncRequestDTO;
import com.flashsale.aiservice.service.KnowledgeSyncService;
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

    @Override
    public void run(ApplicationArguments args) {
        try {
            knowledgeSyncService.sync(new KnowledgeSyncRequestDTO());
            log.info("Initial knowledge sync completed");
        } catch (Exception ex) {
            log.warn("Initial knowledge sync skipped: {}", ex.getMessage());
        }
    }
}
