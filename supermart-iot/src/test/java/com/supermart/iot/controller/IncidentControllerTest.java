package com.supermart.iot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermart.iot.dto.request.AssignTechnicianRequest;
import com.supermart.iot.dto.request.CreateIncidentRequest;
import com.supermart.iot.dto.request.UpdateIncidentStatusRequest;
import com.supermart.iot.dto.response.*;
import com.supermart.iot.enums.IncidentStatus;
import com.supermart.iot.enums.IncidentType;
import com.supermart.iot.exception.ResourceNotFoundException;
import com.supermart.iot.repository.IotDeviceRepository;
import com.supermart.iot.repository.UserRepository;
import com.supermart.iot.security.JwtService;
import com.supermart.iot.service.impl.IncidentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for {@link IncidentController}.
 *
 * <p>Uses {@code @WebMvcTest} to test the HTTP layer in isolation,
 * verifying status codes, content type, and JSON response structure.</p>
 */
@WebMvcTest(IncidentController.class)
class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IncidentService incidentService;

    /** Required by SecurityConfig → DeviceKeyAuthFilter dependency. */
    @MockBean
    private IotDeviceRepository iotDeviceRepository;

    /** Required by SecurityConfig → UserDetailsService dependency. */
    @MockBean
    private UserRepository userRepository;

    /** Required by JwtAuthenticationFilter dependency in security filter chain. */
    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/incidents";

    // ─── GET /incidents ───────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /incidents returns 200 with paged incident list")
    void should_return_200_when_list_incidents_called() throws Exception {
        // given
        IotDeviceSummaryResponse deviceSummary = IotDeviceSummaryResponse.builder()
                .deviceId(9001L).deviceSerial("DEV-9001").build();
        IncidentResponse incidentResponse = IncidentResponse.builder()
                .incidentId(3301L)
                .deviceId(9001L)
                .incidentType(IncidentType.TEMP_EXCEEDED)
                .status(IncidentStatus.OPEN)
                .description("Temp exceeded.")
                .createdAt(LocalDateTime.now())
                .device(deviceSummary)
                .build();

        PagedResponse<IncidentResponse> paged = PagedResponse.<IncidentResponse>builder()
                .content(List.of(incidentResponse))
                .totalElements(1L)
                .totalPages(1)
                .page(0)
                .size(20)
                .build();

        when(incidentService.listIncidents(any(), any(), any(), eq(0), eq(20))).thenReturn(paged);

        // when / then
        mockMvc.perform(get(BASE_URL).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].incidentId").value(3301));
    }

    // ─── POST /incidents ──────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("POST /incidents returns 201 when incident created successfully")
    void should_return_201_when_incident_created_successfully() throws Exception {
        // given
        CreateIncidentRequest request = CreateIncidentRequest.builder()
                .deviceId(9001L)
                .incidentType(IncidentType.TEMP_EXCEEDED)
                .description("Temperature exceeded.")
                .build();

        IotDeviceSummaryResponse deviceSummary = IotDeviceSummaryResponse.builder()
                .deviceId(9001L).build();
        IncidentResponse response = IncidentResponse.builder()
                .incidentId(3301L)
                .deviceId(9001L)
                .incidentType(IncidentType.TEMP_EXCEEDED)
                .status(IncidentStatus.OPEN)
                .device(deviceSummary)
                .createdAt(LocalDateTime.now())
                .build();

        when(incidentService.createIncident(any(CreateIncidentRequest.class))).thenReturn(response);

        // when / then
        mockMvc.perform(post(BASE_URL)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.incidentId").value(3301));
    }

    // ─── GET /incidents/{id} ──────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /incidents/{id} returns 200 when incident found")
    void should_return_200_when_incident_found_by_id() throws Exception {
        // given
        IotDeviceSummaryResponse deviceSummary = IotDeviceSummaryResponse.builder().deviceId(9001L).build();
        IncidentResponse response = IncidentResponse.builder()
                .incidentId(3301L)
                .deviceId(9001L)
                .status(IncidentStatus.OPEN)
                .incidentType(IncidentType.TEMP_EXCEEDED)
                .device(deviceSummary)
                .assignments(Collections.emptyList())
                .createdAt(LocalDateTime.now())
                .build();

        when(incidentService.getIncidentById(3301L)).thenReturn(response);

        // when / then
        mockMvc.perform(get(BASE_URL + "/3301"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.incidentId").value(3301))
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /incidents/{id} returns 404 when incident not found")
    void should_return_404_when_incident_not_found_by_id() throws Exception {
        // given
        when(incidentService.getIncidentById(9999L))
                .thenThrow(new ResourceNotFoundException("Incident with id 9999 not found."));

        // when / then
        mockMvc.perform(get(BASE_URL + "/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    // ─── PUT /incidents/{id}/status ───────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("PUT /incidents/{id}/status returns 200 when status updated")
    void should_return_200_when_incident_status_updated() throws Exception {
        // given
        UpdateIncidentStatusRequest request = UpdateIncidentStatusRequest.builder()
                .status(IncidentStatus.RESOLVED)
                .build();

        IotDeviceSummaryResponse deviceSummary = IotDeviceSummaryResponse.builder().deviceId(9001L).build();
        IncidentResponse response = IncidentResponse.builder()
                .incidentId(3301L)
                .deviceId(9001L)
                .status(IncidentStatus.RESOLVED)
                .incidentType(IncidentType.TEMP_EXCEEDED)
                .device(deviceSummary)
                .resolvedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        when(incidentService.updateStatus(eq(3301L), any(UpdateIncidentStatusRequest.class))).thenReturn(response);

        // when / then
        mockMvc.perform(put(BASE_URL + "/3301/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));
    }

    // ─── POST /incidents/{id}/assign ──────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("POST /incidents/{id}/assign returns 201 when technician assigned")
    void should_return_201_when_technician_assigned_to_incident() throws Exception {
        // given
        AssignTechnicianRequest request = AssignTechnicianRequest.builder()
                .technicianId(55L)
                .notes("Urgent repair.")
                .build();

        TechnicianResponse technicianResponse = TechnicianResponse.builder()
                .technicianId(55L)
                .fullName("Carlos Rivera")
                .build();

        TechnicianAssignmentResponse response = TechnicianAssignmentResponse.builder()
                .assignmentId(1L)
                .incidentId(3301L)
                .technician(technicianResponse)
                .assignedAt(LocalDateTime.now())
                .notes("Urgent repair.")
                .build();

        when(incidentService.assignTechnician(eq(3301L), any(AssignTechnicianRequest.class))).thenReturn(response);

        // when / then
        mockMvc.perform(post(BASE_URL + "/3301/assign")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.technician.fullName").value("Carlos Rivera"))
                .andExpect(jsonPath("$.data.incidentId").value(3301));
    }
}
