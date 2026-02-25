package com.supermart.iot.controller;

import com.supermart.iot.dto.request.AssignTechnicianRequest;
import com.supermart.iot.dto.request.CreateIncidentRequest;
import com.supermart.iot.dto.request.UpdateIncidentStatusRequest;
import com.supermart.iot.dto.response.*;
import com.supermart.iot.enums.IncidentStatus;
import com.supermart.iot.enums.IncidentType;
import com.supermart.iot.service.impl.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/incidents")
@RequiredArgsConstructor
@Tag(name = "Incidents", description = "Incident tracking and management")
public class IncidentController {

    private final IncidentService incidentService;

    @GetMapping
    @Operation(summary = "List incidents (paginated)")
    public ResponseEntity<ApiResponse<PagedResponse<IncidentResponse>>> listIncidents(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) IncidentType incidentType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                incidentService.listIncidents(status, storeId, incidentType, page, size)));
    }

    @PostMapping
    @Operation(summary = "Manually create an incident")
    public ResponseEntity<ApiResponse<IncidentResponse>> createIncident(
            @Valid @RequestBody CreateIncidentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(incidentService.createIncident(request), "Incident created successfully."));
    }

    @GetMapping("/{incidentId}")
    @Operation(summary = "Get incident by ID")
    public ResponseEntity<ApiResponse<IncidentResponse>> getIncident(@PathVariable Long incidentId) {
        return ResponseEntity.ok(ApiResponse.ok(incidentService.getIncidentById(incidentId)));
    }

    @PutMapping("/{incidentId}/status")
    @Operation(summary = "Update incident status")
    public ResponseEntity<ApiResponse<IncidentResponse>> updateStatus(
            @PathVariable Long incidentId,
            @Valid @RequestBody UpdateIncidentStatusRequest request) {
        IncidentResponse response = incidentService.updateStatus(incidentId, request);
        return ResponseEntity.ok(ApiResponse.ok(response,
                "Incident status updated to " + request.getStatus() + "."));
    }

    @PostMapping("/{incidentId}/assign")
    @Operation(summary = "Assign a technician to an incident")
    public ResponseEntity<ApiResponse<TechnicianAssignmentResponse>> assignTechnician(
            @PathVariable Long incidentId,
            @Valid @RequestBody AssignTechnicianRequest request) {
        TechnicianAssignmentResponse response = incidentService.assignTechnician(incidentId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response,
                        "Technician " + response.getTechnician().getFullName() +
                        " assigned to incident " + incidentId + "."));
    }
}
