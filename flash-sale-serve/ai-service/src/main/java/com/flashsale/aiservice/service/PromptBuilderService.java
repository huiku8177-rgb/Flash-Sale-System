package com.flashsale.aiservice.service;

import com.flashsale.aiservice.service.model.PromptBuildRequest;
import com.flashsale.aiservice.service.model.PromptMessageBundle;

public interface PromptBuilderService {

    PromptMessageBundle buildPromptBundle(PromptBuildRequest request);
}
