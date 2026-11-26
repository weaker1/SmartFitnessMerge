package com.smartfitness;

import com.smartfitness.dto.UserDTO;
import com.smartfitness.entity.User;
import com.smartfitness.entity.UserRole;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UserDtoTests {
    @Test
    void userDtoContainsOnlySafeIdentityFields() {
        User user = new User()
                .setId(7L)
                .setUsername("student01")
                .setPassword("should-never-be-copied")
                .setRealName("测试学生")
                .setRole(UserRole.STUDENT);

        UserDTO dto = UserDTO.from(user);

        assertEquals(user.getId(), dto.getId());
        assertEquals(user.getUsername(), dto.getUsername());
        assertEquals(user.getRealName(), dto.getRealName());
        assertEquals(user.getRole().name(), dto.getRole());
        assertFalse(Arrays.stream(UserDTO.class.getDeclaredFields())
                .anyMatch(field -> "password".equals(field.getName())));
    }
}
