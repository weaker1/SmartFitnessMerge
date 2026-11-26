package com.smartfitness.utils;

import com.smartfitness.dto.UserDTO;
import com.smartfitness.entity.UserRole;
import com.smartfitness.exception.BusinessException;

public final class RoleUtils {
    private RoleUtils() {
    }

    public static UserDTO currentUser() {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            throw new BusinessException("用户未登录");
        }
        return user;
    }

    public static UserDTO requireTeacherOrAdmin() {
        UserDTO user = currentUser();
        if (!UserRole.TEACHER.name().equals(user.getRole())
                && !UserRole.ADMIN.name().equals(user.getRole())) {
            throw new BusinessException("无权限执行该操作");
        }
        return user;
    }

    public static UserDTO requireStudent() {
        UserDTO user = currentUser();
        if (!UserRole.STUDENT.name().equals(user.getRole())) {
            throw new BusinessException("仅学生可查询本人记录");
        }
        return user;
    }
}
