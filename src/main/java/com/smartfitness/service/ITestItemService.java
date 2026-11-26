package com.smartfitness.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartfitness.dto.TestItemCreateDTO;
import com.smartfitness.dto.TestItemDTO;
import com.smartfitness.dto.TestItemUpdateDTO;
import com.smartfitness.entity.TestItem;

import java.util.List;

public interface ITestItemService extends IService<TestItem> {
    List<TestItemDTO> listEnabledItems();
    TestItemDTO getItem(Long id);
    Long createItem(TestItemCreateDTO dto);
    void updateItem(Long id, TestItemUpdateDTO dto);
    void updateStatus(Long id, Integer status);
}
