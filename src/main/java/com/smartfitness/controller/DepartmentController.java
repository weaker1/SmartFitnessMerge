package com.smartfitness.controller;

import com.smartfitness.dto.Result;
import com.smartfitness.dto.DepartmentDTO;
import com.smartfitness.service.IDepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/department")
@RequiredArgsConstructor
public class DepartmentController {
    private final IDepartmentService departmentService;

    @GetMapping
    public Result listEnabled() {
        return Result.ok(departmentService.listEnabled().stream()
                .map(DepartmentDTO::from)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Long id) {
        com.smartfitness.entity.Department department = departmentService.getEnabledById(id);
        return Result.ok(department == null ? null : DepartmentDTO.from(department));
    }
}
