package com.smartfitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("tb_student_profile")
public class StudentProfile implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String studentNo;
    private Long departmentId;
    private Long classId;
    private LocalDate birthDate;
    private Integer enrollmentYear;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
