package com.supermart.iot.controller;

import com.supermart.iot.dto.response.*;
import com.supermart.iot.enums.DeviceStatus;
import com.supermart.iot.service.impl.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;

/**
 * REST controller for IoT device management operations.
 *
 * <p>Exposes endpoints for listing devices, retrieving device details,
 * querying telemetry history, and decommissioning retired devices.</p>
 */
@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Devices", description = "IoT device management and telemetry")
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * Lists all IoT devices with optional filtering.
     * DECOMMISSIONED devices are excluded by default (AC-2).
     * Pass {@code includeDecommissioned=true} for audit access (AC-3).
     *
     * @param storeId               optional store ID filter
     * @param status                optional device status filter
     * @param includeDecommissioned when true, includes DECOMMISSIONED devices in results
     * @param page                  zero-based page index (default 0)
     * @param size                  page size (default 20)
     * @return paged list of device summaries
     */
    @GetMapping
    @Operation(summary = "List all IoT devices (paginated)",
               description = "DECOMMISSIONED devices excluded by default. Use includeDecommissioned=true for audit access.")
    public ResponseEntity<ApiResponse<PagedResponse<IotDeviceSummaryResponse>>> listDevices(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) DeviceStatus status,
            @RequestParam(defaultValue = "false") boolean includeDecommissioned,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /devices: storeId={}, status={}, includeDecommissioned={}", storeId, status, includeDecommissioned);
        return ResponseEntity.ok(ApiResponse.ok(
                deviceService.listDevices(storeId, status, includeDecommissioned, page, size)));
    }

    /**
     * Retrieves the full details of a single IoT device by its ID.
     *
     * @param deviceId the unique device identifier
     * @return full device response including equipment unit details
     */
    @GetMapping("/{deviceId}")
    @Operation(summary = "Get IoT device by ID")
    public ResponseEntity<ApiResponse<IotDeviceResponse>> getDevice(@PathVariable Long deviceId) {
        log.info("GET /devices/{}", deviceId);
        return ResponseEntity.ok(ApiResponse.ok(deviceService.getDeviceById(deviceId)));
    }

    /**
     * Returns paginated telemetry history for the specified device.
     *
     * @param deviceId the device ID
     * @param from     optional start of date range (ISO-8601)
     * @param to       optional end of date range (ISO-8601)
     * @param page     zero-based page index (default 0)
     * @param size     page size (default 20)
     * @return paged telemetry records
     */
    @GetMapping("/{deviceId}/telemetry")
    @Operation(summary = "Get telemetry history for a device")
    public ResponseEntity<ApiResponse<PagedResponse<TelemetryResponse>>> getTelemetry(
            @PathVariable Long deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /devices/{}/telemetry: from={}, to={}", deviceId, from, to);
        return ResponseEntity.ok(ApiResponse.ok(deviceService.getDeviceTelemetry(deviceId, from, to, page, size)));
    }

    /**
     * Soft-deletes a retired IoT device by marking it as DECOMMISSIONED (AC-1).
     * Only users with the ADMIN role may call this endpoint (AC-5).
     * Returns 409 CONFLICT if the device has an OPEN incident (AC-4).
     * Writes an audit entry to {@code device_threshold_audit} (AC-6).
     *
     * @param deviceId  the unique ID of the device to decommission
     * @param principal the authenticated principal (injected by Spring Security)
     * @return 204 No Content on success
     */
    @DeleteMapping("/{deviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Decommission (soft-delete) a retired IoT device",
               description = "Sets status=DECOMMISSIONED and stamps decommissionedAt. " +
                             "Requires ADMIN role. Returns 409 if an OPEN incident exists.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204",
            description = "Device successfully decommissioned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
            description = "Device not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
            description = "Cannot decommission — device has an open incident"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
            description = "Forbidden — ADMIN role required")
    })
    public ResponseEntity<Void> decommissionDevice(@PathVariable Long deviceId,
                                                   Principal principal) {
        log.info("DELETE /devices/{}: principal={}", deviceId, principal.getName());
        deviceService.decommissionDevice(deviceId, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
