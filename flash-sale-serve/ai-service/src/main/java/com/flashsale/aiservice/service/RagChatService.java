package com.flashsale.aiservice.service;

import com.flashsale.aiservice.domain.dto.ChatRequestDTO;
import com.flashsale.aiservice.domain.vo.ChatResponseVO;
import com.flashsale.aiservice.domain.vo.ChatSessionVO;
import com.flashsale.aiservice.domain.vo.ChatSessionSummaryVO;

import java.util.List;

public interface RagChatService {

    ChatResponseVO chat(Long userId, ChatRequestDTO request);

    ChatSessionVO getSession(Long userId, String sessionId);

    List<ChatSessionSummaryVO> listSessions(Long userId, Integer limit);

    void deleteSession(Long userId, String sessionId);
}
