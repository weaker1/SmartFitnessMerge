package com.smartfitness.dto;

import com.smartfitness.entity.TestItem;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TestItemDTO {
    private Long id;
    private String name;
    private String description;
    private String unit;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static TestItemDTO from(TestItem item) {
        TestItemDTO dto = new TestItemDTO();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setUnit(item.getUnit());
        dto.setStatus(item.getStatus());
        dto.setCreateTime(item.getCreateTime());
        dto.setUpdateTime(item.getUpdateTime());
        return dto;
    }
}
