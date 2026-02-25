package com.supermart.iot.controller;

import com.supermart.iot.dto.request.TelemetryIngestRequest;
import com.supermart.iot.dto.response.ApiResponse;
import com.supermart.iot.dto.response.TelemetryResponse;
import com.supermart.iot.service.impl.TelemetryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/telemetry")
@RequiredArgsConstructor
@Tag(name = "Telemetry", description = "IoT device data ingestion")
public class TelemetryController {

    private final TelemetryService telemetryService;

    @PostMapping
    @Operation(summary = "Ingest telemetry reading from an IoT device")
    public ResponseEntity<ApiResponse<TelemetryResponse>> ingest(
            @Valid @RequestBody TelemetryIngestRequest request) {
        TelemetryResponse record = telemetryService.ingest(request);
        String message = record.getIsAlert()
                ? "Telemetry recorded. Temperature threshold exceeded — incident created."
                : "Telemetry recorded successfully.";
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(record, message));
    }
}
