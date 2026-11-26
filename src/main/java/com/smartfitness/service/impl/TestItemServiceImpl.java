package com.smartfitness.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smartfitness.dto.TestItemCreateDTO;
import com.smartfitness.dto.TestItemDTO;
import com.smartfitness.dto.TestItemUpdateDTO;
import com.smartfitness.entity.TestItem;
import com.smartfitness.exception.BusinessException;
import com.smartfitness.mapper.TestItemMapper;
import com.smartfitness.service.ITestItemService;
import com.smartfitness.service.TestItemCacheService;
import com.smartfitness.utils.RoleUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TestItemServiceImpl extends ServiceImpl<TestItemMapper, TestItem> implements ITestItemService {
    private final TestItemMapper testItemMapper;
    private final TestItemCacheService cacheService;

    public TestItemServiceImpl(TestItemMapper testItemMapper, TestItemCacheService cacheService) {
        this.testItemMapper = testItemMapper;
        this.cacheService = cacheService;
    }

    @Override
    public List<TestItemDTO> listEnabledItems() {
        return testItemMapper.selectList(new QueryWrapper<TestItem>()
                        .eq("status", 1).orderByAsc("id"))
                .stream().map(TestItemDTO::from).collect(Collectors.toList());
    }

    @Override
    public TestItemDTO getItem(Long id) {
        return cacheService.getById(id);
    }

    @Override
    @Transactional
    public Long createItem(TestItemCreateDTO dto) {
        Long operatorId = RoleUtils.requireTeacherOrAdmin().getId();
        ensureNameAvailable(dto.getName(), null);
        TestItem item = new TestItem()
                .setName(dto.getName().trim())
                .setDescription(dto.getDescription())
                .setUnit(dto.getUnit().trim())
                .setCreatedBy(operatorId)
                .setStatus(1);
        testItemMapper.insert(item);
        cacheService.evictAfterCommit(item.getId());
        return item.getId();
    }

    @Override
    @Transactional
    public void updateItem(Long id, TestItemUpdateDTO dto) {
        RoleUtils.requireTeacherOrAdmin();
        requireItem(id);
        ensureNameAvailable(dto.getName(), id);
        TestItem update = new TestItem()
                .setId(id)
                .setName(dto.getName().trim())
                .setDescription(dto.getDescription())
                .setUnit(dto.getUnit().trim());
        testItemMapper.updateById(update);
        cacheService.evictAfterCommit(id);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        RoleUtils.requireTeacherOrAdmin();
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("项目状态只能为0或1");
        }
        requireItem(id);
        testItemMapper.updateById(new TestItem().setId(id).setStatus(status));
        cacheService.evictAfterCommit(id);
    }

    private TestItem requireItem(Long id) {
        TestItem item = testItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException("体测项目不存在");
        }
        return item;
    }

    private void ensureNameAvailable(String name, Long excludedId) {
        Integer count = testItemMapper.selectCount(new QueryWrapper<TestItem>()
                .eq("name", name.trim())
                .ne(excludedId != null, "id", excludedId));
        if (count > 0) {
            throw new BusinessException("体测项目名称已存在");
        }
    }
}
