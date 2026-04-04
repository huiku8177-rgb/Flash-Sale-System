package com.flashsale.aiservice.service;

import com.flashsale.aiservice.domain.dto.ChatRequestDTO;
import com.flashsale.aiservice.domain.vo.ChatResponseVO;
import com.flashsale.aiservice.domain.vo.ChatSessionVO;

public interface RagChatService {

    ChatResponseVO chat(Long userId, ChatRequestDTO request);

    ChatSessionVO getSession(Long userId, String sessionId);
}
