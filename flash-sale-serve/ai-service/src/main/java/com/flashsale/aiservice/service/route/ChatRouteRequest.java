package com.flashsale.aiservice.service.route;

import com.flashsale.aiservice.domain.dto.ChatRequestDTO;
import com.flashsale.aiservice.domain.enums.OutOfScopeTopicType;
import com.flashsale.aiservice.domain.enums.QuestionCategory;
import com.flashsale.aiservice.domain.enums.QuestionIntentType;
import com.flashsale.aiservice.domain.po.ChatRecordPO;
import com.flashsale.aiservice.domain.po.ChatSessionPO;
import com.flashsale.aiservice.domain.vo.ConversationContextState;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChatRouteRequest {

    private Long userId;
    private ChatRequestDTO originalRequest;
    private ChatSessionPO session;
    private Long currentProductId;
    private String currentProductName;
    private String currentProductType;
    private String question;
    private String rewrittenQuestion;
    private QuestionIntentType intentType;
    private OutOfScopeTopicType outOfScopeTopicType;
    private QuestionCategory category;
    private ConversationContextState contextState;
    private List<ChatRecordPO> history = new ArrayList<>();
}
