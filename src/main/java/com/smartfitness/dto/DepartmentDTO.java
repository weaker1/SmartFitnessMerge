package com.smartfitness.dto;

import com.smartfitness.entity.Department;
import lombok.Data;

@Data
public class DepartmentDTO {
    private Long id;
    private String code;
    private String name;

    public static DepartmentDTO from(Department department) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(department.getId());
        dto.setCode(department.getCode());
        dto.setName(department.getName());
        return dto;
    }
}
