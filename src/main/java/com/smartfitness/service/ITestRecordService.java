package com.smartfitness.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartfitness.dto.TestRecordCreateDTO;
import com.smartfitness.dto.TestRecordVO;
import com.smartfitness.entity.TestRecord;

import java.time.LocalDateTime;

public interface ITestRecordService extends IService<TestRecord> {
    Long createRecord(TestRecordCreateDTO dto);
    Page<TestRecordVO> pageMyRecords(Long itemId, long current, long pageSize);
    Page<TestRecordVO> pageStudentRecords(Long studentUserId, Long itemId,
                                          LocalDateTime startTime, LocalDateTime endTime,
                                          long current, long pageSize);
}
