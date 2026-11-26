package com.smartfitness.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartfitness.dto.AppointmentCancellationResult;
import com.smartfitness.dto.UserDTO;
import com.smartfitness.dto.AppointmentRequestVO;
import com.smartfitness.dto.AppointmentRequestState;
import com.smartfitness.entity.RedisReserveResult;
import com.smartfitness.entity.AppointmentStatus;
import com.smartfitness.entity.TestAppointment;
import com.smartfitness.exception.BusinessException;
import com.smartfitness.mapper.*;
import com.smartfitness.service.impl.TestAppointmentServiceImpl;
import com.smartfitness.utils.UserHolder;
import org.junit.jupiter.api.*;
import java.time.LocalDateTime;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TestAppointmentServiceImplTests {
    TestAppointmentMapper appointmentMapper; AppointmentRedisService redisService;
    AppointmentMysqlTransactionService transactionService; TestSessionMapper sessionMapper;
    AppointmentRequestStatusService requestStatusService; AppointmentMessagePublisher messagePublisher;
    RedisRateLimitService rateLimitService;
    TestAppointmentServiceImpl service;

    @BeforeEach void setUp() {
        appointmentMapper=mock(TestAppointmentMapper.class); redisService=mock(AppointmentRedisService.class);
        transactionService=mock(AppointmentMysqlTransactionService.class); sessionMapper=mock(TestSessionMapper.class);
        requestStatusService=mock(AppointmentRequestStatusService.class); messagePublisher=mock(AppointmentMessagePublisher.class);
        rateLimitService=mock(RedisRateLimitService.class);
        service=new TestAppointmentServiceImpl(appointmentMapper,sessionMapper,mock(TestItemMapper.class),
                mock(UserMapper.class),mock(StudentProfileMapper.class),redisService,transactionService,
                requestStatusService,messagePublisher,rateLimitService);
    }
    @AfterEach void clear(){UserHolder.removeUser();}

    @Test void redisSuccessThenMessageAccepted(){login(1L,"STUDENT");
        when(redisService.reserve(anyLong(),anyLong(),anyLong())).thenReturn(RedisReserveResult.SUCCESS);
        when(messagePublisher.publish(any())).thenReturn(new AppointmentRequestVO("r1","ACCEPTED","ok"));
        assertEquals("ACCEPTED",service.bookSession(5L).getStatus()); verify(messagePublisher).publish(any());}

    @Test void luaFailuresNeverReachMysql(){login(1L,"STUDENT");
        for(RedisReserveResult failure:new RedisReserveResult[]{RedisReserveResult.NOT_INITIALIZED,RedisReserveResult.NOT_PUBLISHED,
                RedisReserveResult.NOT_STARTED,RedisReserveResult.ENDED,RedisReserveResult.DUPLICATE,RedisReserveResult.NO_STOCK}){
            when(redisService.reserve(anyLong(),anyLong(),anyLong())).thenReturn(failure);
            assertThrows(BusinessException.class,()->service.bookSession(5L));}
        verify(messagePublisher,never()).publish(any());}

    @Test void appointmentRateLimitRunsBeforeReserveAndDoesNotReplaceLua(){login(1L,"STUDENT");
        doThrow(new BusinessException("请求过于频繁，请稍后重试")).when(rateLimitService).checkAppointment(1L);
        assertThrows(BusinessException.class,()->service.bookSession(5L));
        verify(redisService,never()).reserve(anyLong(),anyLong(),anyLong());}

    @Test void publishingFailureTriggersRedisRollback(){login(1L,"STUDENT");
        when(redisService.reserve(anyLong(),anyLong(),anyLong())).thenReturn(RedisReserveResult.SUCCESS);
        doThrow(new BusinessException("redis status fail")).when(requestStatusService)
                .createPublishing(anyString(),anyLong(),anyLong());
        when(redisService.rollback(5L,1L)).thenReturn(true);
        assertThrows(BusinessException.class,()->service.bookSession(5L)); verify(redisService).rollback(5L,1L);}

    @Test void teacherAndAdminCannotEnterRedis(){login(2L,"TEACHER");
        assertThrows(BusinessException.class,()->service.bookSession(5L));
        UserHolder.removeUser(); login(3L,"ADMIN");
        assertThrows(BusinessException.class,()->service.bookSession(5L));
        verify(redisService,never()).reserve(anyLong(),anyLong(),anyLong());}

    @Test void normalCancelUpdatesRedisWithoutRemovingQualification(){login(1L,"STUDENT");
        when(transactionService.cancelAppointment(anyLong(),anyLong(),any())).thenReturn(new AppointmentCancellationResult(5L,1L));
        when(redisService.cancel(5L,1L)).thenReturn(true); service.cancelAppointment(9L);
        verify(redisService).cancel(5L,1L); verify(redisService,never()).rollback(anyLong(),anyLong());}

    @Test void cancelRedisFailureRetriesThreeTimes(){login(1L,"STUDENT");
        when(transactionService.cancelAppointment(anyLong(),anyLong(),any())).thenReturn(new AppointmentCancellationResult(5L,1L));
        when(redisService.cancel(5L,1L)).thenThrow(new RuntimeException("down"));
        assertThrows(BusinessException.class,()->service.cancelAppointment(9L)); verify(redisService,times(3)).cancel(5L,1L);}

    @Test void myAppointmentsUsesCurrentStudent(){login(77L,"STUDENT"); Page<TestAppointment> p=new Page<>(1,10,0);p.setRecords(Collections.emptyList());
        when(appointmentMapper.selectPage(any(Page.class),any(Wrapper.class))).thenReturn(p); service.pageMyAppointments(1,10);
        verify(appointmentMapper).selectPage(any(Page.class),any(Wrapper.class));}

    @Test void teacherAndAdminCanViewSessionRoster(){Page<TestAppointment> p=new Page<>(1,10,0);p.setRecords(Collections.emptyList());
        when(sessionMapper.selectById(5L)).thenReturn(new com.smartfitness.entity.TestSession().setId(5L));
        when(appointmentMapper.selectPage(any(Page.class),any(Wrapper.class))).thenReturn(p);
        login(2L,"TEACHER"); service.pageSessionAppointments(5L,1,10);
        UserHolder.removeUser(); login(3L,"ADMIN"); service.pageSessionAppointments(5L,1,10);
        verify(appointmentMapper,times(2)).selectPage(any(Page.class),any(Wrapper.class));}

    @Test void studentCannotQueryAnotherStudentsRequest(){login(77L,"STUDENT");
        when(requestStatusService.get("r1")).thenReturn(new AppointmentRequestState()
                .setRequestId("r1").setUserId(88L).setSessionId(5L).setStatus("ACCEPTED"));
        assertThrows(BusinessException.class,()->service.getRequestStatus("r1"));}

    @Test void expiredRedisRequestFallsBackToBookedDatabaseRecord(){login(77L,"STUDENT");
        when(requestStatusService.get("r1")).thenReturn(null);
        when(transactionService.findByRequestId("r1")).thenReturn(new TestAppointment()
                .setRequestId("r1").setUserId(77L).setStatus(AppointmentStatus.BOOKED.getCode()));
        AppointmentRequestVO result=service.getRequestStatus("r1");
        assertEquals("BOOKED",result.getStatus()); assertEquals("状态来自数据库",result.getMessage());}

    @Test void expiredRedisRequestFallsBackToCancelledDatabaseRecord(){login(77L,"STUDENT");
        when(requestStatusService.get("r2")).thenReturn(null);
        when(transactionService.findByRequestId("r2")).thenReturn(new TestAppointment()
                .setRequestId("r2").setUserId(77L).setStatus(AppointmentStatus.CANCELLED.getCode()));
        assertEquals("CANCELLED",service.getRequestStatus("r2").getStatus());}

    @Test void expiredUnknownRequestIsRejectedOnlyWhenRedisAndDatabaseAreMissing(){login(77L,"STUDENT");
        when(requestStatusService.get("missing")).thenReturn(null);
        when(transactionService.findByRequestId("missing")).thenReturn(null);
        assertThrows(BusinessException.class,()->service.getRequestStatus("missing"));}

    private void login(Long id,String role){UserDTO u=new UserDTO();u.setId(id);u.setRole(role);UserHolder.saveUser(u);}
}
