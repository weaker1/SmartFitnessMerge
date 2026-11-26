package com.smartfitness.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartfitness.entity.StudentProfile;

public interface IStudentProfileService extends IService<StudentProfile> {
    StudentProfile getByUserId(Long userId);
}
