package com.smartfitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartfitness.entity.TestSession;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface TestSessionMapper extends BaseMapper<TestSession> {
    @Select("SELECT * FROM tb_test_session WHERE id = #{sessionId} FOR UPDATE")
    TestSession selectByIdForUpdate(@Param("sessionId") Long sessionId);

    @Update("UPDATE tb_test_session " +
            "SET available_quota = available_quota - 1 " +
            "WHERE id = #{sessionId} " +
            "AND available_quota > 0 " +
            "AND status = 1 " +
            "AND appointment_start_time <= #{now} " +
            "AND appointment_end_time >= #{now}")
    int decrementQuotaIfAvailable(@Param("sessionId") Long sessionId, @Param("now") LocalDateTime now);

    @Update("UPDATE tb_test_session " +
            "SET available_quota = available_quota + 1 " +
            "WHERE id = #{sessionId} AND available_quota < capacity")
    int incrementQuota(@Param("sessionId") Long sessionId);
}
