package com.smartfitness.controller;

import com.smartfitness.dto.Result;
import com.smartfitness.dto.FitnessClassDTO;
import com.smartfitness.service.IFitnessClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/fitness-class")
@RequiredArgsConstructor
public class FitnessClassController {
    private final IFitnessClassService fitnessClassService;

    @GetMapping
    public Result listByDepartment(@RequestParam Long departmentId) {
        return Result.ok(fitnessClassService.listByDepartmentId(departmentId).stream()
                .map(FitnessClassDTO::from)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Long id) {
        com.smartfitness.entity.FitnessClass fitnessClass = fitnessClassService.getById(id);
        return Result.ok(fitnessClass == null ? null : FitnessClassDTO.from(fitnessClass));
    }
}
