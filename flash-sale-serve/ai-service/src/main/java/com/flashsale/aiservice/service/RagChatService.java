package com.flashsale.aiservice.service;

import com.flashsale.aiservice.domain.dto.ChatRequestDTO;
import com.flashsale.aiservice.domain.vo.ChatResponseVO;

public interface RagChatService {

    ChatResponseVO chat(ChatRequestDTO request);
}
