package com.flashsale.aiservice.service.route.strategy;

import com.flashsale.aiservice.domain.enums.QuestionIntentType;
import com.flashsale.aiservice.service.impl.RagRouteExecutionService;
import com.flashsale.aiservice.service.route.ChatRouteRequest;
import com.flashsale.aiservice.service.route.ChatRouteResult;
import com.flashsale.aiservice.service.route.ChatRouteStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PolicyQaStrategy implements ChatRouteStrategy {

    private final RagRouteExecutionService executionService;

    @Override
    public QuestionIntentType supports() {
        return QuestionIntentType.POLICY_QA;
    }

    @Override
    public ChatRouteResult execute(ChatRouteRequest request) {
        return executionService.policyQa(request);
    }
}
