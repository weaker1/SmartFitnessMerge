package com.smartfitness.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartfitness.dto.Result;
import com.smartfitness.dto.TestRecordCreateDTO;
import com.smartfitness.dto.TestRecordVO;
import com.smartfitness.service.ITestRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/test-record")
@RequiredArgsConstructor
public class TestRecordController {
    private final ITestRecordService testRecordService;

    @PostMapping
    public Result create(@Valid @RequestBody TestRecordCreateDTO dto) {
        return Result.ok(testRecordService.createRecord(dto));
    }

    @GetMapping("/me")
    public Result myRecords(@RequestParam(required = false) Long itemId,
                            @RequestParam(defaultValue = "1") long current,
                            @RequestParam(defaultValue = "10") long pageSize) {
        Page<TestRecordVO> page = testRecordService.pageMyRecords(itemId, current, pageSize);
        return Result.ok(page.getRecords(), page.getTotal());
    }

    @GetMapping("/student/{studentUserId}")
    public Result studentRecords(@PathVariable Long studentUserId,
                                 @RequestParam(required = false) Long itemId,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                                 @RequestParam(defaultValue = "1") long current,
                                 @RequestParam(defaultValue = "10") long pageSize) {
        Page<TestRecordVO> page = testRecordService.pageStudentRecords(
                studentUserId, itemId, startTime, endTime, current, pageSize);
        return Result.ok(page.getRecords(), page.getTotal());
    }
}
