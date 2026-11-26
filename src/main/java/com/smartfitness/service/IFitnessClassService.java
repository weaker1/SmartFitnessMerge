package com.smartfitness.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartfitness.entity.FitnessClass;

import java.util.List;

public interface IFitnessClassService extends IService<FitnessClass> {
    List<FitnessClass> listByDepartmentId(Long departmentId);
}
