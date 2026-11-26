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
@TableName("tb_test_session")
public class TestSession implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long itemId;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime appointmentStartTime;
    private LocalDateTime appointmentEndTime;
    private Integer capacity;
    private Integer availableQuota;
    private Integer status;
    private Long createdBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
