package com.flashsale.aiservice.mapper;

import com.flashsale.aiservice.domain.po.ChatRecordPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ChatRecordMapper {

    int insert(ChatRecordPO record);

    Integer nextRecordNo(@Param("sessionId") String sessionId);

    List<ChatRecordPO> listBySessionId(@Param("sessionId") String sessionId, @Param("limit") Integer limit);

    List<ChatRecordPO> listRecentBySessionId(@Param("sessionId") String sessionId, @Param("limit") Integer limit);

    int deleteBySessionId(@Param("sessionId") String sessionId);

    long countRecords();

    int deleteExpired(@Param("deadline") LocalDateTime deadline);
}
