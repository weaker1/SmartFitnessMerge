package com.smartfitness.service;

import com.smartfitness.dto.RegisterUserDTO;
import com.smartfitness.entity.StudentProfile;
import com.smartfitness.entity.User;
import com.smartfitness.entity.UserRole;
import com.smartfitness.entity.Department;
import com.smartfitness.entity.FitnessClass;
import com.smartfitness.mapper.DepartmentMapper;
import com.smartfitness.mapper.FitnessClassMapper;
import com.smartfitness.mapper.StudentProfileMapper;
import com.smartfitness.mapper.UserMapper;
import com.smartfitness.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTests {
    @Mock
    private UserMapper userMapper;
    @Mock
    private StudentProfileMapper studentProfileMapper;
    @Mock
    private DepartmentMapper departmentMapper;
    @Mock
    private FitnessClassMapper fitnessClassMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private BCryptPasswordEncoder passwordEncoder;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserServiceImpl(userMapper, studentProfileMapper, departmentMapper,
                fitnessClassMapper, stringRedisTemplate, passwordEncoder);
    }

    @Test
    void registrationAlwaysCreatesStudentAndHashesPassword() {
        when(userMapper.selectCount(any())).thenReturn(0);
        when(studentProfileMapper.selectCount(any())).thenReturn(0);
        when(departmentMapper.selectById(10L)).thenReturn(new Department().setId(10L).setStatus(1));
        when(fitnessClassMapper.selectById(20L)).thenReturn(new FitnessClass().setId(20L).setDepartmentId(10L));
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(100L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        RegisterUserDTO request = new RegisterUserDTO();
        request.setUsername("student01");
        request.setPassword("PlainPassword123");
        request.setRealName("测试学生");
        request.setStudentNo("20260001");
        request.setDepartmentId(10L);
        request.setClassId(20L);

        userService.register(request);

        org.mockito.ArgumentCaptor<User> userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        org.mockito.ArgumentCaptor<StudentProfile> profileCaptor = org.mockito.ArgumentCaptor.forClass(StudentProfile.class);
        verify(userMapper).insert(userCaptor.capture());
        verify(studentProfileMapper).insert(profileCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(UserRole.STUDENT, savedUser.getRole());
        assertNotEquals(request.getPassword(), savedUser.getPassword());
        assertTrue(passwordEncoder.matches(request.getPassword(), savedUser.getPassword()));
        assertEquals(savedUser.getId(), profileCaptor.getValue().getUserId());
    }
}
