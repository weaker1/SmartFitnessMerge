package com.smartfitness.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartfitness.entity.FitnessClass;
import com.smartfitness.mapper.FitnessClassMapper;
import com.smartfitness.service.IFitnessClassService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FitnessClassServiceImpl extends ServiceImpl<FitnessClassMapper, FitnessClass> implements IFitnessClassService {
    @Override
    public List<FitnessClass> listByDepartmentId(Long departmentId) {
        return query().eq("department_id", departmentId).orderByDesc("grade_year").orderByAsc("name").list();
    }
}
