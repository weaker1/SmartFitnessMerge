package com.smartfitness.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartfitness.entity.Department;

import java.util.List;

public interface IDepartmentService extends IService<Department> {
    List<Department> listEnabled();
    Department getEnabledById(Long id);
}
