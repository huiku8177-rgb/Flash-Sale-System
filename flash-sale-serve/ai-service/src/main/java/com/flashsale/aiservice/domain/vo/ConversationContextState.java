package com.flashsale.aiservice.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ConversationContextState {

    private Long currentProductId;
    private String currentProductName;
    // Persist product type so pronoun follow-up can still distinguish normal vs seckill products.
    private String currentProductType;
    private String currentIntentType;
    private String lastQuestion;
    private String lastAnswerSummary;
    private List<Long> compareCandidateIds = new ArrayList<>();
    private List<String> compareCandidateNames = new ArrayList<>();
    private String rewrittenQuestion;
}
