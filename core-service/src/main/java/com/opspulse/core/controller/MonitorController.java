package com.opspulse.core.controller;

import com.opspulse.core.dto.CreateMonitorRequest;
import com.opspulse.core.dto.MonitorResponse;
import com.opspulse.core.dto.UpdateMonitorRequest;
import com.opspulse.core.service.MonitorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/monitors")
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @PostMapping
    public ResponseEntity<MonitorResponse> createMonitor(@Valid @RequestBody CreateMonitorRequest request) {
        MonitorResponse response = monitorService.createMonitor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<MonitorResponse>> listMonitors() {
        return ResponseEntity.ok(monitorService.listMonitors());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MonitorResponse> getMonitor(@PathVariable UUID id) {
        return ResponseEntity.ok(monitorService.getMonitor(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MonitorResponse> updateMonitor(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMonitorRequest request) {
        return ResponseEntity.ok(monitorService.updateMonitor(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMonitor(@PathVariable UUID id) {
        monitorService.deleteMonitor(id);
        return ResponseEntity.noContent().build();
    }
}
