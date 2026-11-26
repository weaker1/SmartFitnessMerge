package com.smartfitness.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartfitness.dto.TestSessionCreateDTO;
import com.smartfitness.dto.TestSessionUpdateDTO;
import com.smartfitness.dto.TestSessionVO;
import com.smartfitness.entity.TestSession;

public interface ITestSessionService extends IService<TestSession> {
    Long createSession(TestSessionCreateDTO dto);
    void updateDraftSession(Long id, TestSessionUpdateDTO dto);
    void publishSession(Long id);
    void closeSession(Long id);
    void cancelSession(Long id);
    TestSessionVO getSession(Long id);
    Page<TestSessionVO> pageVisibleSessions(Long itemId, long current, long pageSize);
}
