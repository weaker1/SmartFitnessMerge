package com.smartfitness.controller;

import com.smartfitness.dto.Result;
import com.smartfitness.dto.UserDTO;
import com.smartfitness.service.IStudentProfileService;
import com.smartfitness.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/student-profile")
@RequiredArgsConstructor
public class StudentProfileController {
    private final IStudentProfileService studentProfileService;

    @GetMapping("/me")
    public Result me() {
        UserDTO currentUser = UserHolder.getUser();
        return Result.ok(studentProfileService.getByUserId(currentUser.getId()));
    }
}
