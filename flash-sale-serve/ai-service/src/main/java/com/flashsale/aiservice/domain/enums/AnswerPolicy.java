package com.flashsale.aiservice.domain.enums;

public enum AnswerPolicy {
    FIXED_TEMPLATE,
    RAG_MODEL,
    RAG_FALLBACK_NO_KNOWLEDGE,
    RAG_FALLBACK_MODEL_ERROR,
    REALTIME_ONLY,
    OUT_OF_SCOPE_REFUSAL
}
