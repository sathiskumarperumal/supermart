package com.supermart.iot.controller;

import com.supermart.iot.dto.response.*;
import com.supermart.iot.enums.DeviceStatus;
import com.supermart.iot.service.impl.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
@Tag(name = "Devices", description = "IoT device management and telemetry")
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    @Operation(summary = "List all IoT devices (paginated)")
    public ResponseEntity<ApiResponse<PagedResponse<IotDeviceSummaryResponse>>> listDevices(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) DeviceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(deviceService.listDevices(storeId, status, page, size)));
    }

    @GetMapping("/{deviceId}")
    @Operation(summary = "Get IoT device by ID")
    public ResponseEntity<ApiResponse<IotDeviceResponse>> getDevice(@PathVariable Long deviceId) {
        return ResponseEntity.ok(ApiResponse.ok(deviceService.getDeviceById(deviceId)));
    }

    @GetMapping("/{deviceId}/telemetry")
    @Operation(summary = "Get telemetry history for a device")
    public ResponseEntity<ApiResponse<PagedResponse<TelemetryResponse>>> getTelemetry(
            @PathVariable Long deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(deviceService.getDeviceTelemetry(deviceId, from, to, page, size)));
    }
}
