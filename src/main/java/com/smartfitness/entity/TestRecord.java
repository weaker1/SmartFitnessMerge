package com.smartfitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("tb_test_record")
public class TestRecord implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long itemId;
    private BigDecimal score;
    private String videoUrl;
    private String dataUrl;
    private LocalDateTime testTime;
    private Long createdBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
