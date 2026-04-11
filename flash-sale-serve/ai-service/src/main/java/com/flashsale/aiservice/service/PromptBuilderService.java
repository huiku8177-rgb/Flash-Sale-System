package com.flashsale.aiservice.service;

import com.flashsale.aiservice.service.model.PromptBuildRequest;

public interface PromptBuilderService {

    String buildPrompt(PromptBuildRequest request);
}
