package com.flashsale.aiservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.aiservice.domain.po.ChatRecordPO;
import com.flashsale.aiservice.domain.vo.RelatedKnowledgeVO;
import com.flashsale.aiservice.exception.AiServiceException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChatJsonCodec {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<RelatedKnowledgeVO>> KNOWLEDGE_LIST_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<ChatRecordPO>> CHAT_RECORD_LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public ChatJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String writeStringList(List<String> values) {
        return writeValue(values);
    }

    public String writeKnowledgeList(List<RelatedKnowledgeVO> values) {
        return writeValue(values);
    }

    public String writeRecordList(List<ChatRecordPO> values) {
        return writeValue(values);
    }

    public List<String> readStringList(String value) {
        return readValue(value, STRING_LIST_TYPE);
    }

    public List<RelatedKnowledgeVO> readKnowledgeList(String value) {
        return readValue(value, KNOWLEDGE_LIST_TYPE);
    }

    public List<ChatRecordPO> readRecordList(String value) {
        return readValue(value, CHAT_RECORD_LIST_TYPE);
    }

    private String writeValue(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new AiServiceException("Failed to serialize chat payload", ex);
        }
    }

    private <T> T readValue(String value, TypeReference<T> typeReference) {
        if (value == null || value.isBlank()) {
            return readEmpty(typeReference);
        }
        try {
            return objectMapper.readValue(value, typeReference);
        } catch (JsonProcessingException ex) {
            throw new AiServiceException("Failed to deserialize chat payload", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T readEmpty(TypeReference<T> typeReference) {
        if (typeReference == STRING_LIST_TYPE || typeReference == KNOWLEDGE_LIST_TYPE || typeReference == CHAT_RECORD_LIST_TYPE) {
            return (T) List.of();
        }
        return null;
    }
}
