package com.flashsale.aiservice.service.impl;

import com.flashsale.aiservice.config.AiProperties;
import com.flashsale.aiservice.domain.po.ChatRecordPO;
import com.flashsale.aiservice.domain.po.ChatSessionPO;
import com.flashsale.aiservice.domain.vo.ChatRecordVO;
import com.flashsale.aiservice.domain.vo.ChatSessionVO;
import com.flashsale.aiservice.mapper.ChatRecordMapper;
import com.flashsale.aiservice.service.ChatCacheService;
import com.flashsale.aiservice.service.ChatRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ChatRecordServiceImpl implements ChatRecordService {

    private final ChatRecordMapper chatRecordMapper;
    private final ChatCacheService chatCacheService;
    private final ChatJsonCodec chatJsonCodec;
    private final AiProperties aiProperties;

    public ChatRecordServiceImpl(ChatRecordMapper chatRecordMapper, ChatCacheService chatCacheService,
                                 ChatJsonCodec chatJsonCodec, AiProperties aiProperties) {
        this.chatRecordMapper = chatRecordMapper;
        this.chatCacheService = chatCacheService;
        this.chatJsonCodec = chatJsonCodec;
        this.aiProperties = aiProperties;
    }

    @Override
    @Transactional
    public ChatRecordPO save(ChatRecordPO record) {
        Integer nextRecordNo = chatRecordMapper.nextRecordNo(record.getSessionId());
        record.setRecordNo(nextRecordNo == null ? 1 : nextRecordNo);
        chatRecordMapper.insert(record);

        // Keep only recent history in Redis for cheap multi-turn context assembly.
        List<ChatRecordPO> recentHistory = chatRecordMapper.listRecentBySessionId(record.getSessionId(), aiProperties.getHistoryCacheSize());
        List<ChatRecordPO> normalizedRecords = new ArrayList<>(recentHistory);
        normalizedRecords.sort(Comparator.comparing(ChatRecordPO::getRecordNo));
        chatCacheService.cacheRecentHistory(record.getSessionId(), normalizedRecords);
        return record;
    }

    @Override
    public List<ChatRecordPO> listRecentHistory(String sessionId, int limit) {
        List<ChatRecordPO> cachedRecords = chatCacheService.getRecentHistory(sessionId);
        if (!cachedRecords.isEmpty()) {
            List<ChatRecordPO> sortedRecords = cachedRecords.stream()
                    .sorted(Comparator.comparing(ChatRecordPO::getRecordNo))
                    .toList();
            if (sortedRecords.size() <= limit) {
                return sortedRecords;
            }
            return sortedRecords.subList(sortedRecords.size() - limit, sortedRecords.size());
        }

        List<ChatRecordPO> records = chatRecordMapper.listRecentBySessionId(sessionId, limit);
        List<ChatRecordPO> normalizedRecords = new ArrayList<>(records);
        normalizedRecords.sort(Comparator.comparing(ChatRecordPO::getRecordNo));
        if (!normalizedRecords.isEmpty()) {
            chatCacheService.cacheRecentHistory(sessionId, normalizedRecords);
        }
        return normalizedRecords;
    }

    @Override
    public ChatSessionVO getSessionDetail(ChatSessionPO session, int limit) {
        List<ChatRecordPO> records = chatRecordMapper.listBySessionId(session.getSessionId(), limit);
        ChatSessionVO sessionVO = new ChatSessionVO();
        sessionVO.setSessionId(session.getSessionId());
        sessionVO.setUserId(session.getUserId());
        sessionVO.setProductId(session.getProductId());
        sessionVO.setContextType(session.getContextType());
        sessionVO.setMessageCount(session.getMessageCount());
        sessionVO.setCreatedAt(session.getCreatedAt());
        sessionVO.setLastActiveAt(session.getLastActiveAt());
        sessionVO.setRecords(records.stream().map(this::toChatRecordVO).toList());
        return sessionVO;
    }

    @Override
    public long countRecords() {
        return chatRecordMapper.countRecords();
    }

    @Override
    @Transactional
    public int deleteExpired(LocalDateTime deadline) {
        return chatRecordMapper.deleteExpired(deadline);
    }

    private ChatRecordVO toChatRecordVO(ChatRecordPO record) {
        ChatRecordVO vo = new ChatRecordVO();
        vo.setRecordNo(record.getRecordNo());
        vo.setQuestion(record.getQuestion());
        vo.setQuestionCategory(record.getQuestionCategory());
        vo.setAnswer(record.getAnswer());
        vo.setAnswerPolicy(record.getAnswerPolicy());
        vo.setSources(chatJsonCodec.readStringList(record.getSourcesJson()));
        vo.setHitKnowledge(chatJsonCodec.readKnowledgeList(record.getHitKnowledgeJson()));
        vo.setConfidence(record.getConfidence());
        vo.setFallbackReason(record.getFallbackReason());
        vo.setAuditSummary(record.getAuditSummary());
        vo.setCreatedAt(record.getCreatedAt());
        return vo;
    }
}
