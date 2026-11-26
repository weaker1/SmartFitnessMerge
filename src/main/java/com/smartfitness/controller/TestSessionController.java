package com.smartfitness.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartfitness.dto.Result;
import com.smartfitness.dto.TestSessionCreateDTO;
import com.smartfitness.dto.TestSessionUpdateDTO;
import com.smartfitness.dto.TestSessionVO;
import com.smartfitness.service.ITestSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/test-session")
@RequiredArgsConstructor
public class TestSessionController {
    private final ITestSessionService sessionService;

    @PostMapping
    public Result create(@Valid @RequestBody TestSessionCreateDTO dto) {
        return Result.ok(sessionService.createSession(dto));
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @Valid @RequestBody TestSessionUpdateDTO dto) {
        sessionService.updateDraftSession(id, dto);
        return Result.ok();
    }

    @PutMapping("/{id}/publish")
    public Result publish(@PathVariable Long id) {
        sessionService.publishSession(id);
        return Result.ok();
    }

    @PutMapping("/{id}/close")
    public Result close(@PathVariable Long id) {
        sessionService.closeSession(id);
        return Result.ok();
    }

    @PutMapping("/{id}/cancel")
    public Result cancel(@PathVariable Long id) {
        sessionService.cancelSession(id);
        return Result.ok();
    }

    @GetMapping("/{id}")
    public Result detail(@PathVariable Long id) {
        return Result.ok(sessionService.getSession(id));
    }

    @GetMapping
    public Result visibleSessions(@RequestParam(required = false) Long itemId,
                                  @RequestParam(defaultValue = "1") long current,
                                  @RequestParam(defaultValue = "10") long pageSize) {
        Page<TestSessionVO> page = sessionService.pageVisibleSessions(itemId, current, pageSize);
        return Result.ok(page.getRecords(), page.getTotal());
    }
}
