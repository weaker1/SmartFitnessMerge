package com.smartfitness.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@Data
public class RegisterUserDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 50, message = "用户名长度必须为4到50位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度必须为8到64位")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名不能超过50位")
    private String realName;

    @NotBlank(message = "学号不能为空")
    @Size(max = 32, message = "学号不能超过32位")
    private String studentNo;

    @NotNull(message = "学院不能为空")
    private Long departmentId;

    @NotNull(message = "班级不能为空")
    private Long classId;

    private Integer gender;
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;
    private LocalDate birthDate;
    private Integer enrollmentYear;
}
