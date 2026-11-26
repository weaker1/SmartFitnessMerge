package com.smartfitness.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartfitness.dto.LoginFormDTO;
import com.smartfitness.dto.RegisterUserDTO;
import com.smartfitness.dto.Result;
import com.smartfitness.entity.User;

public interface IUserService extends IService<User> {
    Result register(RegisterUserDTO registerUserDTO);
    Result login(LoginFormDTO loginFormDTO);
    Result logout(String token);
}
