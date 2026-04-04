package com.flashsale.aiservice.service;

import com.flashsale.aiservice.domain.po.ChatRecordPO;
import com.flashsale.aiservice.domain.po.ChatSessionPO;
import com.flashsale.aiservice.domain.vo.ChatSessionVO;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatRecordService {

    ChatRecordPO save(ChatRecordPO record);

    List<ChatRecordPO> listRecentHistory(String sessionId, int limit);

    ChatSessionVO getSessionDetail(ChatSessionPO session, int limit);

    long countRecords();

    int deleteExpired(LocalDateTime deadline);
}
