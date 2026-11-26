package com.smartfitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("tb_test_appointment")
public class TestAppointment implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String requestId;
    private Long userId;
    private Long sessionId;
    private Integer status;
    private LocalDateTime bookedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
