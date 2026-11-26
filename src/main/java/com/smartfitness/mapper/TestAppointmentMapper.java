package com.smartfitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartfitness.entity.TestAppointment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface TestAppointmentMapper extends BaseMapper<TestAppointment> {
    @Update("UPDATE tb_test_appointment SET status = 2, cancelled_at = #{cancelledAt} " +
            "WHERE id = #{appointmentId} AND user_id = #{userId} AND status = 1")
    int cancelBookedAppointment(@Param("appointmentId") Long appointmentId,
                                @Param("userId") Long userId,
                                @Param("cancelledAt") LocalDateTime cancelledAt);
}
