package com.smartfitness.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.smartfitness.dto.TestItemCreateDTO;
import com.smartfitness.dto.UserDTO;
import com.smartfitness.entity.TestItem;
import com.smartfitness.exception.BusinessException;
import com.smartfitness.mapper.TestItemMapper;
import com.smartfitness.service.impl.TestItemServiceImpl;
import com.smartfitness.utils.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TestItemServiceImplTests {
    private TestItemMapper mapper;
    private TestItemCacheService cacheService;
    private TestItemServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(TestItemMapper.class);
        cacheService = mock(TestItemCacheService.class);
        service = new TestItemServiceImpl(mapper, cacheService);
    }

    @AfterEach
    void clearUser() {
        UserHolder.removeUser();
    }

    @Test
    void studentCannotCreateItem() {
        login(1L, "STUDENT");
        assertThrows(BusinessException.class, () -> service.createItem(createDto()));
        verify(mapper, never()).insert(any(TestItem.class));
    }

    @Test
    void teacherCanCreateItemAndCreatedByComesFromContext() {
        login(8L, "TEACHER");
        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0);
        service.createItem(createDto());

        ArgumentCaptor<TestItem> captor = ArgumentCaptor.forClass(TestItem.class);
        verify(mapper).insert(captor.capture());
        assertEquals(8L, captor.getValue().getCreatedBy());
        assertEquals(1, captor.getValue().getStatus());
        verify(cacheService).evictAfterCommit(captor.getValue().getId());
    }

    @Test
    void adminCanCreateItem() {
        login(9L, "ADMIN");
        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0);
        service.createItem(createDto());
        verify(mapper).insert(any(TestItem.class));
    }

    @Test
    void adminCanStopItem() {
        login(9L, "ADMIN");
        when(mapper.selectById(2L)).thenReturn(new TestItem().setId(2L).setStatus(1));
        service.updateStatus(2L, 0);
        verify(mapper).updateById(any(TestItem.class));
        verify(cacheService).evictAfterCommit(2L);
    }

    private TestItemCreateDTO createDto() {
        TestItemCreateDTO dto = new TestItemCreateDTO();
        dto.setName("坐位体前屈");
        dto.setUnit("cm");
        return dto;
    }

    private void login(Long id, String role) {
        UserDTO user = new UserDTO();
        user.setId(id);
        user.setRole(role);
        UserHolder.saveUser(user);
    }
}
