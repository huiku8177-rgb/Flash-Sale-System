package com.flashsale.aiservice.controller;

import com.flashsale.aiservice.domain.dto.ChatRequestDTO;
import com.flashsale.aiservice.domain.vo.ChatResponseVO;
import com.flashsale.aiservice.service.RagChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "AI问答", description = "AI智能客服问答接口")
@RequestMapping("/ai/chat")
public class ChatController {

    private final RagChatService ragChatService;

    @Operation(summary = "AI问答", description = "智能客服问答接口，支持商品相关问题咨询")
    @PostMapping
    public ResponseEntity<ChatResponseVO> chat(@Valid @RequestBody ChatRequestDTO request) {
        log.info("收到AI问答请求: {}", request.getQuestion());
        
        ChatResponseVO response = ragChatService.chat(request);
        
        log.info("AI问答完成，回答长度: {}", response.getAnswer().length());
        return ResponseEntity.ok(response);
    }
}
