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
@TableName("tb_fitness_class")
public class FitnessClass implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long departmentId;
    private String name;
    private Integer gradeYear;
    private Long teacherId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
