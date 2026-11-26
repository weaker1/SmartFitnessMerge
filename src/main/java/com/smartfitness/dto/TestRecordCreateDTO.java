package com.smartfitness.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TestRecordCreateDTO {
    @NotNull(message = "学生用户ID不能为空")
    private Long userId;
    @NotNull(message = "体测项目ID不能为空")
    private Long itemId;
    @NotNull(message = "体测成绩不能为空")
    @DecimalMin(value = "0", message = "体测成绩不能小于0")
    private BigDecimal score;
    @Size(max = 500, message = "视频地址不能超过500个字符")
    private String videoUrl;
    @Size(max = 500, message = "数据地址不能超过500个字符")
    private String dataUrl;
    @NotNull(message = "体测时间不能为空")
    @PastOrPresent(message = "体测时间不能晚于当前时间")
    private LocalDateTime testTime;
}
