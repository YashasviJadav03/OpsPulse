package com.opspulse.core.service;

import com.opspulse.core.dto.CreateMonitorRequest;
import com.opspulse.core.dto.MonitorResponse;
import com.opspulse.core.dto.UpdateMonitorRequest;

import java.util.List;
import java.util.UUID;

public interface MonitorService {
    MonitorResponse createMonitor(CreateMonitorRequest request);
    List<MonitorResponse> listMonitors();
    MonitorResponse getMonitor(UUID id);
    MonitorResponse updateMonitor(UUID id, UpdateMonitorRequest request);
    void deleteMonitor(UUID id);
}
