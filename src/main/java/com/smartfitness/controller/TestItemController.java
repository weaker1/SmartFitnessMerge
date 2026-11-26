package com.smartfitness.controller;

import com.smartfitness.dto.Result;
import com.smartfitness.dto.TestItemCreateDTO;
import com.smartfitness.dto.TestItemUpdateDTO;
import com.smartfitness.service.ITestItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/test-item")
@RequiredArgsConstructor
public class TestItemController {
    private final ITestItemService testItemService;

    @GetMapping
    public Result listEnabled() {
        return Result.ok(testItemService.listEnabledItems());
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Long id) {
        return Result.ok(testItemService.getItem(id));
    }

    @PostMapping
    public Result create(@Valid @RequestBody TestItemCreateDTO dto) {
        return Result.ok(testItemService.createItem(dto));
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @Valid @RequestBody TestItemUpdateDTO dto) {
        testItemService.updateItem(id, dto);
        return Result.ok();
    }

    @PutMapping("/{id}/status/{status}")
    public Result updateStatus(@PathVariable Long id,
                               @PathVariable @Min(value = 0, message = "状态只能为0或1")
                               @Max(value = 1, message = "状态只能为0或1") Integer status) {
        testItemService.updateStatus(id, status);
        return Result.ok();
    }
}
