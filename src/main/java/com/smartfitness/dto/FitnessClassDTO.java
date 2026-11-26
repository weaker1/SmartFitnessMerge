package com.smartfitness.dto;

import com.smartfitness.entity.FitnessClass;
import lombok.Data;

@Data
public class FitnessClassDTO {
    private Long id;
    private Long departmentId;
    private String name;
    private Integer gradeYear;

    public static FitnessClassDTO from(FitnessClass fitnessClass) {
        FitnessClassDTO dto = new FitnessClassDTO();
        dto.setId(fitnessClass.getId());
        dto.setDepartmentId(fitnessClass.getDepartmentId());
        dto.setName(fitnessClass.getName());
        dto.setGradeYear(fitnessClass.getGradeYear());
        return dto;
    }
}
