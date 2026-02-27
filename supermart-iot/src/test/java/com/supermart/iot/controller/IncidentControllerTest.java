package com.supermart.iot.controller;

import com.supermart.iot.dto.request.AssignTechnicianRequest;
import com.supermart.iot.dto.request.CreateIncidentRequest;
import com.supermart.iot.dto.request.UpdateIncidentStatusRequest;
import com.supermart.iot.dto.response.*;
import com.supermart.iot.enums.IncidentStatus;
import com.supermart.iot.enums.IncidentType;
import com.supermart.iot.service.impl.IncidentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentControllerTest {

    @Mock
    private IncidentService incidentService;

    @InjectMocks
    private IncidentController incidentController;

    private IncidentResponse buildIncidentResponse() {
        return IncidentResponse.builder()
                .incidentId(1L).deviceId(10L)
                .incidentType(IncidentType.TEMP_EXCEEDED)
                .status(IncidentStatus.OPEN)
                .description("Test incident")
                .createdAt(LocalDateTime.now())
                .assignments(Collections.emptyList())
                .build();
    }

    @Test
    void listIncidents_returnsOkWithPagedResponse() {
        PagedResponse<IncidentResponse> paged = PagedResponse.<IncidentResponse>builder()
                .content(Collections.singletonList(buildIncidentResponse()))
                .page(0).size(20).totalElements(1).totalPages(1).build();
        when(incidentService.listIncidents(any(), any(), any(), anyInt(), anyInt())).thenReturn(paged);

        ResponseEntity<ApiResponse<PagedResponse<IncidentResponse>>> response =
                incidentController.listIncidents(null, null, null, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getContent()).hasSize(1);
    }

    @Test
    void createIncident_returns201WithCreatedResponse() {
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setDeviceId(10L);
        request.setIncidentType(IncidentType.TEMP_EXCEEDED);

        IncidentResponse incidentResponse = buildIncidentResponse();
        when(incidentService.createIncident(request)).thenReturn(incidentResponse);

        ResponseEntity<ApiResponse<IncidentResponse>> response = incidentController.createIncident(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("created");
    }

    @Test
    void getIncident_returnsOkWithIncidentResponse() {
        IncidentResponse incidentResponse = buildIncidentResponse();
        when(incidentService.getIncidentById(1L)).thenReturn(incidentResponse);

        ResponseEntity<ApiResponse<IncidentResponse>> response = incidentController.getIncident(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getIncidentId()).isEqualTo(1L);
    }

    @Test
    void updateStatus_returnsOkWithUpdatedIncident() {
        UpdateIncidentStatusRequest request = new UpdateIncidentStatusRequest();
        request.setStatus(IncidentStatus.RESOLVED);
        IncidentResponse incidentResponse = IncidentResponse.builder()
                .incidentId(1L).deviceId(10L).status(IncidentStatus.RESOLVED)
                .incidentType(IncidentType.TEMP_EXCEEDED).createdAt(LocalDateTime.now())
                .assignments(Collections.emptyList()).build();

        when(incidentService.updateStatus(1L, request)).thenReturn(incidentResponse);

        ResponseEntity<ApiResponse<IncidentResponse>> response = incidentController.updateStatus(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("RESOLVED");
    }

    @Test
    void assignTechnician_returns201WithAssignmentResponse() {
        AssignTechnicianRequest request = new AssignTechnicianRequest();
        request.setTechnicianId(5L);
        request.setNotes("On-site visit needed");

        TechnicianResponse techResponse = TechnicianResponse.builder()
                .technicianId(5L).fullName("John Tech").build();
        TechnicianAssignmentResponse assignmentResponse = TechnicianAssignmentResponse.builder()
                .assignmentId(1L).incidentId(1L).technician(techResponse)
                .assignedAt(LocalDateTime.now()).build();

        when(incidentService.assignTechnician(1L, request)).thenReturn(assignmentResponse);

        ResponseEntity<ApiResponse<TechnicianAssignmentResponse>> response =
                incidentController.assignTechnician(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("John Tech");
    }
}
