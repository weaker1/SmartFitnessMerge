package com.smartfitness.service;

import com.smartfitness.entity.AppointmentRequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AppointmentRequestStatusServiceTests {
    private CapturingStatusService service;

    @BeforeEach
    void setUp() {
        service = new CapturingStatusService();
    }

    @Test
    void lateAcceptedCanOnlyReplacePublishingNotBooked() {
        assertTrue(service.markAccepted("r1"));
        assertEquals(AppointmentRequestStatus.ACCEPTED, service.target);
        assertEquals(Arrays.asList(AppointmentRequestStatus.PUBLISHING), service.allowed);
        assertFalse(service.allowed.contains(AppointmentRequestStatus.BOOKED));
    }

    @Test
    void lateUnknownCanOnlyReplacePublishingNotBooked() {
        assertTrue(service.markUnknown("r1", "timeout"));
        assertEquals(AppointmentRequestStatus.UNKNOWN, service.target);
        assertEquals(Arrays.asList(AppointmentRequestStatus.PUBLISHING), service.allowed);
        assertFalse(service.allowed.contains(AppointmentRequestStatus.BOOKED));
    }

    @Test
    void consumerMayAdvancePublishingAcceptedOrUnknownToBooked() {
        assertTrue(service.markBooked("r1"));
        assertEquals(AppointmentRequestStatus.BOOKED, service.target);
        assertTrue(service.allowed.containsAll(Arrays.asList(AppointmentRequestStatus.PUBLISHING,
                AppointmentRequestStatus.ACCEPTED, AppointmentRequestStatus.UNKNOWN,
                AppointmentRequestStatus.BOOKED)));
    }

    private static class CapturingStatusService extends AppointmentRequestStatusService {
        private AppointmentRequestStatus target;
        private List<AppointmentRequestStatus> allowed;

        CapturingStatusService() {
            super(mock(StringRedisTemplate.class));
        }

        @Override
        boolean transition(String requestId, AppointmentRequestStatus target, String reason,
                           AppointmentRequestStatus... allowed) {
            this.target = target;
            this.allowed = Arrays.asList(allowed);
            return true;
        }
    }
}
