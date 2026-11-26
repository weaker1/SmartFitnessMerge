package com.smartfitness.dto;

import com.smartfitness.entity.TestItem;
import com.smartfitness.entity.TestRecord;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TestRecordVO {
    private Long id;
    private Long userId;
    private Long itemId;
    private String itemName;
    private String unit;
    private BigDecimal score;
    private String videoUrl;
    private String dataUrl;
    private LocalDateTime testTime;
    private Long createdBy;
    private LocalDateTime createTime;

    public static TestRecordVO from(TestRecord record, TestItem item) {
        TestRecordVO vo = new TestRecordVO();
        vo.setId(record.getId());
        vo.setUserId(record.getUserId());
        vo.setItemId(record.getItemId());
        vo.setScore(record.getScore());
        vo.setVideoUrl(record.getVideoUrl());
        vo.setDataUrl(record.getDataUrl());
        vo.setTestTime(record.getTestTime());
        vo.setCreatedBy(record.getCreatedBy());
        vo.setCreateTime(record.getCreateTime());
        if (item != null) {
            vo.setItemName(item.getName());
            vo.setUnit(item.getUnit());
        }
        return vo;
    }
}
