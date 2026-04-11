package com.flashsale.aiservice.service.route;

import com.flashsale.aiservice.domain.enums.QuestionIntentType;

public interface ChatRouteStrategy {

    QuestionIntentType supports();

    ChatRouteResult execute(ChatRouteRequest request);
}
