package com.flashsale.aiservice.mapper;

import com.flashsale.aiservice.domain.po.ChatSessionPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ChatSessionMapper {

    int insert(ChatSessionPO session);

    ChatSessionPO getBySessionId(@Param("sessionId") String sessionId);

    List<ChatSessionPO> listByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);

    int updateActivity(ChatSessionPO session);

    int deleteBySessionId(@Param("sessionId") String sessionId);

    long countSessions();

    int deleteExpired(@Param("deadline") LocalDateTime deadline);
}
