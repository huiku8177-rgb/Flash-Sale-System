package com.flashsale.aiservice.task;

import com.flashsale.aiservice.service.ChatRecordService;
import com.flashsale.aiservice.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatDataCleanupScheduler {

    private final ChatRecordService chatRecordService;
    private final ChatSessionService chatSessionService;

    @Scheduled(
            initialDelayString = "${ai.cleanup.initial-delay-ms:60000}",
            fixedDelayString = "${ai.cleanup.fixed-delay-ms:3600000}"
    )
    public void cleanupExpiredChatData() {
        // Redis entries expire by TTL; scheduler only needs to clear MySQL truth data.
        LocalDateTime deadline = LocalDateTime.now();
        int deletedRecords = chatRecordService.deleteExpired(deadline);
        int deletedSessions = chatSessionService.deleteExpired(deadline);
        if (deletedRecords > 0 || deletedSessions > 0) {
            log.info("Cleaned expired chat data, sessions={}, records={}", deletedSessions, deletedRecords);
        }
    }
}
