package com.flashsale.aiservice.mapper;

import com.flashsale.aiservice.domain.po.ChatSessionPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface ChatSessionMapper {

    int insert(ChatSessionPO session);

    ChatSessionPO getBySessionId(@Param("sessionId") String sessionId);

    int updateActivity(ChatSessionPO session);

    long countSessions();

    int deleteExpired(@Param("deadline") LocalDateTime deadline);
}
