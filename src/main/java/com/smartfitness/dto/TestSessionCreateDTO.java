package com.smartfitness.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
public class TestSessionCreateDTO {
    @NotNull(message = "体测项目ID不能为空")
    private Long itemId;
    @NotBlank(message = "场次标题不能为空")
    @Size(max = 100, message = "场次标题不能超过100个字符")
    private String title;
    @NotNull(message = "场次开始时间不能为空")
    private LocalDateTime startTime;
    @NotNull(message = "场次结束时间不能为空")
    private LocalDateTime endTime;
    @NotNull(message = "预约开始时间不能为空")
    private LocalDateTime appointmentStartTime;
    @NotNull(message = "预约结束时间不能为空")
    private LocalDateTime appointmentEndTime;
    @NotNull(message = "场次容量不能为空")
    @Min(value = 1, message = "场次容量必须大于0")
    private Integer capacity;
}
