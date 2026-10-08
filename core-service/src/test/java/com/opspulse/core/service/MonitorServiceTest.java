package com.opspulse.core.service;

import com.opspulse.common.exception.NotFoundException;
import com.opspulse.common.tenant.TenantContext;
import com.opspulse.core.domain.Monitor;
import com.opspulse.core.dto.CreateMonitorRequest;
import com.opspulse.core.dto.MonitorResponse;
import com.opspulse.core.dto.UpdateMonitorRequest;
import com.opspulse.core.repository.MonitorRepository;
import com.opspulse.core.service.impl.MonitorServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitorServiceTest {

    @Mock
    private MonitorRepository monitorRepository;

    @InjectMocks
    private MonitorServiceImpl monitorService;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldCreateMonitorWithCurrentTenant() {
        CreateMonitorRequest request = new CreateMonitorRequest(
                "API Health",
                "https://api.example.com/health",
                "GET",
                60,
                200,
                true
        );

        Monitor savedMonitor = new Monitor(tenantId, request.name(), request.url(), "GET", 60, 200, true);
        when(monitorRepository.save(any(Monitor.class))).thenReturn(savedMonitor);

        MonitorResponse response = monitorService.createMonitor(request);

        assertNotNull(response);
        assertEquals("API Health", response.name());
        assertEquals("https://api.example.com/health", response.url());
        assertEquals(tenantId, response.tenantId());

        ArgumentCaptor<Monitor> captor = ArgumentCaptor.forClass(Monitor.class);
        verify(monitorRepository).save(captor.capture());
        assertEquals(tenantId, captor.getValue().getTenantId());
    }

    @Test
    void shouldListMonitors() {
        Monitor monitor1 = new Monitor(tenantId, "M1", "https://m1.com", "GET", 60, 200, true);
        Monitor monitor2 = new Monitor(tenantId, "M2", "https://m2.com", "GET", 120, 200, true);
        when(monitorRepository.findAll()).thenReturn(List.of(monitor1, monitor2));

        List<MonitorResponse> result = monitorService.listMonitors();

        assertEquals(2, result.size());
        assertEquals("M1", result.get(0).name());
        assertEquals("M2", result.get(1).name());
        verify(monitorRepository).findAll();
    }

    @Test
    void shouldGetMonitorById() {
        UUID monitorId = UUID.randomUUID();
        Monitor monitor = new Monitor(tenantId, "M1", "https://m1.com", "GET", 60, 200, true);
        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(monitor));

        MonitorResponse response = monitorService.getMonitor(monitorId);

        assertNotNull(response);
        assertEquals("M1", response.name());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenMonitorDoesNotExist() {
        UUID monitorId = UUID.randomUUID();
        when(monitorRepository.findById(monitorId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> monitorService.getMonitor(monitorId));
        assertTrue(exception.getMessage().contains(monitorId.toString()));
    }

    @Test
    void shouldUpdateMonitor() {
        UUID monitorId = UUID.randomUUID();
        Monitor existing = new Monitor(tenantId, "Old Name", "https://old.com", "GET", 60, 200, true);
        UpdateMonitorRequest updateRequest = new UpdateMonitorRequest(
                "New Name",
                "https://new.com",
                "POST",
                120,
                201,
                false
        );

        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(existing));
        when(monitorRepository.save(any(Monitor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MonitorResponse response = monitorService.updateMonitor(monitorId, updateRequest);

        assertEquals("New Name", response.name());
        assertEquals("https://new.com", response.url());
        assertEquals("POST", response.method());
        assertEquals(120, response.intervalSeconds());
        assertEquals(201, response.expectedStatus());
        assertFalse(response.enabled());
    }

    @Test
    void shouldDeleteMonitor() {
        UUID monitorId = UUID.randomUUID();
        Monitor existing = new Monitor(tenantId, "M1", "https://m1.com", "GET", 60, 200, true);
        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(existing));

        monitorService.deleteMonitor(monitorId);

        verify(monitorRepository).delete(existing);
    }

    @Test
    void shouldFailWhenTenantContextIsMissing() {
        TenantContext.clear();
        CreateMonitorRequest request = new CreateMonitorRequest(
                "M1",
                "https://m1.com",
                "GET",
                60,
                200,
                true
        );

        assertThrows(IllegalStateException.class, () -> monitorService.createMonitor(request));
    }
}
