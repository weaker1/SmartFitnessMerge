package com.smartfitness.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartfitness.entity.Department;
import com.smartfitness.mapper.DepartmentMapper;
import com.smartfitness.service.IDepartmentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements IDepartmentService {
    @Override
    public List<Department> listEnabled() {
        return query().eq("status", 1).orderByAsc("name").list();
    }

    @Override
    public Department getEnabledById(Long id) {
        return query().eq("id", id).eq("status", 1).one();
    }
}
