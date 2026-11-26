package com.smartfitness.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartfitness.dto.Result;
import com.smartfitness.dto.SessionAppointmentVO;
import com.smartfitness.dto.TestAppointmentVO;
import com.smartfitness.service.ITestAppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test-appointment")
@RequiredArgsConstructor
public class TestAppointmentController {
    private final ITestAppointmentService appointmentService;

    @PostMapping("/session/{sessionId}")
    public Result book(@PathVariable Long sessionId) {
        return Result.ok(appointmentService.bookSession(sessionId));
    }

    @GetMapping("/request/{requestId}")
    public Result requestStatus(@PathVariable String requestId) {
        return Result.ok(appointmentService.getRequestStatus(requestId));
    }

    @PutMapping("/{appointmentId}/cancel")
    public Result cancel(@PathVariable Long appointmentId) {
        appointmentService.cancelAppointment(appointmentId);
        return Result.ok();
    }

    @GetMapping("/me")
    public Result myAppointments(@RequestParam(defaultValue = "1") long current,
                                 @RequestParam(defaultValue = "10") long pageSize) {
        Page<TestAppointmentVO> page = appointmentService.pageMyAppointments(current, pageSize);
        return Result.ok(page.getRecords(), page.getTotal());
    }

    @GetMapping("/session/{sessionId}")
    public Result sessionAppointments(@PathVariable Long sessionId,
                                      @RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long pageSize) {
        Page<SessionAppointmentVO> page = appointmentService.pageSessionAppointments(sessionId, current, pageSize);
        return Result.ok(page.getRecords(), page.getTotal());
    }
}
