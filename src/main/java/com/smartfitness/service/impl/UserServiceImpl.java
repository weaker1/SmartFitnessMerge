package com.smartfitness.service.impl;

import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartfitness.dto.LoginFormDTO;
import com.smartfitness.dto.RegisterUserDTO;
import com.smartfitness.dto.Result;
import com.smartfitness.dto.UserDTO;
import com.smartfitness.entity.StudentProfile;
import com.smartfitness.entity.User;
import com.smartfitness.entity.UserRole;
import com.smartfitness.entity.Department;
import com.smartfitness.entity.FitnessClass;
import com.smartfitness.exception.BusinessException;
import com.smartfitness.mapper.StudentProfileMapper;
import com.smartfitness.mapper.UserMapper;
import com.smartfitness.mapper.DepartmentMapper;
import com.smartfitness.mapper.FitnessClassMapper;
import com.smartfitness.service.IUserService;
import com.smartfitness.utils.RedisConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    public static final int STATUS_ENABLED = 1;

    private final UserMapper userMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final DepartmentMapper departmentMapper;
    private final FitnessClassMapper fitnessClassMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result register(RegisterUserDTO request) {
        if (userMapper.selectCount(new QueryWrapper<User>().eq("username", request.getUsername())) > 0) {
            throw new BusinessException("用户名已存在");
        }
        if (studentProfileMapper.selectCount(new QueryWrapper<StudentProfile>()
                .eq("student_no", request.getStudentNo())) > 0) {
            throw new BusinessException("学号已存在");
        }

        Department department = departmentMapper.selectById(request.getDepartmentId());
        if (department == null) {
            throw new BusinessException("学院不存在");
        }
        if (!Integer.valueOf(STATUS_ENABLED).equals(department.getStatus())) {
            throw new BusinessException("学院未启用");
        }
        FitnessClass fitnessClass = fitnessClassMapper.selectById(request.getClassId());
        if (fitnessClass == null) {
            throw new BusinessException("班级不存在");
        }
        if (!request.getDepartmentId().equals(fitnessClass.getDepartmentId())) {
            throw new BusinessException("班级不属于所选学院");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User()
                .setUsername(request.getUsername())
                .setPassword(passwordEncoder.encode(request.getPassword()))
                .setRole(UserRole.STUDENT)
                .setRealName(request.getRealName())
                .setGender(request.getGender())
                .setPhone(request.getPhone())
                .setEmail(request.getEmail())
                .setStatus(STATUS_ENABLED)
                .setCreateTime(now)
                .setUpdateTime(now);

        StudentProfile profile = new StudentProfile()
                .setStudentNo(request.getStudentNo())
                .setDepartmentId(request.getDepartmentId())
                .setClassId(request.getClassId())
                .setBirthDate(request.getBirthDate())
                .setEnrollmentYear(request.getEnrollmentYear())
                .setCreateTime(now)
                .setUpdateTime(now);

        try {
            userMapper.insert(user);
            profile.setUserId(user.getId());
            studentProfileMapper.insert(profile);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("用户名或学号已存在");
        }
        return Result.ok(UserDTO.from(user));
    }

    @Override
    public Result login(LoginFormDTO request) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!Integer.valueOf(STATUS_ENABLED).equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用");
        }

        String token = UUID.randomUUID().toString(true);
        UserDTO userDTO = UserDTO.from(user);
        Map<String, String> userMap = new HashMap<>();
        userMap.put("id", String.valueOf(userDTO.getId()));
        userMap.put("username", userDTO.getUsername());
        userMap.put("realName", userDTO.getRealName());
        userMap.put("role", userDTO.getRole());

        String tokenKey = RedisConstants.LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        stringRedisTemplate.expire(tokenKey, RedisConstants.LOGIN_USER_TTL_MINUTES, TimeUnit.MINUTES);
        return Result.ok(token);
    }

    @Override
    public Result logout(String token) {
        if (token != null) {
            stringRedisTemplate.delete(RedisConstants.LOGIN_USER_KEY + token);
        }
        return Result.ok();
    }
}
