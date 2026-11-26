package com.smartfitness.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartfitness.entity.StudentProfile;
import com.smartfitness.mapper.StudentProfileMapper;
import com.smartfitness.service.IStudentProfileService;
import org.springframework.stereotype.Service;

@Service
public class StudentProfileServiceImpl extends ServiceImpl<StudentProfileMapper, StudentProfile> implements IStudentProfileService {
    @Override
    public StudentProfile getByUserId(Long userId) {
        return query().eq("user_id", userId).one();
    }
}
