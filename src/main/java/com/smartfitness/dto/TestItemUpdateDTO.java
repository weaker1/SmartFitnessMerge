package com.smartfitness.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class TestItemUpdateDTO {
    @NotBlank(message = "体测项目名称不能为空")
    @Size(max = 100, message = "体测项目名称不能超过100个字符")
    private String name;
    @Size(max = 500, message = "项目说明不能超过500个字符")
    private String description;
    @NotBlank(message = "计量单位不能为空")
    @Size(max = 20, message = "计量单位不能超过20个字符")
    private String unit;
}
