package com.opspulse.core.service.impl;

import com.opspulse.common.exception.NotFoundException;
import com.opspulse.common.tenant.TenantContext;
import com.opspulse.core.domain.Monitor;
import com.opspulse.core.dto.CreateMonitorRequest;
import com.opspulse.core.dto.MonitorResponse;
import com.opspulse.core.dto.UpdateMonitorRequest;
import com.opspulse.core.repository.MonitorRepository;
import com.opspulse.core.service.MonitorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MonitorServiceImpl implements MonitorService {

    private final MonitorRepository monitorRepository;

    public MonitorServiceImpl(MonitorRepository monitorRepository) {
        this.monitorRepository = monitorRepository;
    }

    @Override
    public MonitorResponse createMonitor(CreateMonitorRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        Monitor monitor = new Monitor(
                tenantId,
                request.name(),
                request.url(),
                request.resolveMethod(),
                request.intervalSeconds(),
                request.resolveExpectedStatus(),
                request.resolveEnabled()
        );
        Monitor saved = monitorRepository.save(monitor);
        return MonitorResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonitorResponse> listMonitors() {
        UUID tenantId = TenantContext.requireTenantId();
        return monitorRepository.findAll()
                .stream()
                .filter(m -> tenantId.equals(m.getTenantId()))
                .map(MonitorResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MonitorResponse getMonitor(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        Monitor monitor = monitorRepository.findById(id)
                .filter(m -> tenantId.equals(m.getTenantId()))
                .orElseThrow(() -> new NotFoundException("Monitor not found: " + id));
        return MonitorResponse.fromEntity(monitor);
    }

    @Override
    public MonitorResponse updateMonitor(UUID id, UpdateMonitorRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        Monitor monitor = monitorRepository.findById(id)
                .filter(m -> tenantId.equals(m.getTenantId()))
                .orElseThrow(() -> new NotFoundException("Monitor not found: " + id));

        monitor.setName(request.name());
        monitor.setUrl(request.url());
        monitor.setMethod(request.resolveMethod());
        monitor.setIntervalSeconds(request.intervalSeconds());
        monitor.setExpectedStatus(request.expectedStatus());
        monitor.setEnabled(request.enabled());

        Monitor updated = monitorRepository.save(monitor);
        return MonitorResponse.fromEntity(updated);
    }

    @Override
    public void deleteMonitor(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        Monitor monitor = monitorRepository.findById(id)
                .filter(m -> tenantId.equals(m.getTenantId()))
                .orElseThrow(() -> new NotFoundException("Monitor not found: " + id));
        monitorRepository.delete(monitor);
    }
}
