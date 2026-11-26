package com.smartfitness.controller;

import com.smartfitness.dto.LoginFormDTO;
import com.smartfitness.dto.RegisterUserDTO;
import com.smartfitness.dto.Result;
import com.smartfitness.service.IUserService;
import com.smartfitness.service.RedisRateLimitService;
import com.smartfitness.utils.AuthorizationUtils;
import com.smartfitness.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final IUserService userService;
    private final RedisRateLimitService rateLimitService;

    @PostMapping("/register")
    public Result register(@Valid @RequestBody RegisterUserDTO request) {
        return userService.register(request);
    }

    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginFormDTO request, HttpServletRequest httpRequest) {
        rateLimitService.checkLogin(request.getUsername(), clientIp(httpRequest));
        return userService.login(request);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.trim().isEmpty()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    @PostMapping("/logout")
    public Result logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Result result = userService.logout(AuthorizationUtils.extractBearerToken(authorization));
        return result;
    }

    @GetMapping("/me")
    public Result me() {
        return Result.ok(UserHolder.getUser());
    }
}
