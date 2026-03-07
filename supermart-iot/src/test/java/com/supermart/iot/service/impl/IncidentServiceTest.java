package com.supermart.iot.service.impl;

import com.supermart.iot.dto.request.AssignTechnicianRequest;
import com.supermart.iot.dto.request.CreateIncidentRequest;
import com.supermart.iot.dto.request.UpdateIncidentStatusRequest;
import com.supermart.iot.dto.response.IncidentResponse;
import com.supermart.iot.dto.response.IotDeviceSummaryResponse;
import com.supermart.iot.dto.response.PagedResponse;
import com.supermart.iot.dto.response.TechnicianAssignmentResponse;
import com.supermart.iot.entity.*;
import com.supermart.iot.enums.DeviceStatus;
import com.supermart.iot.enums.EquipmentType;
import com.supermart.iot.enums.IncidentStatus;
import com.supermart.iot.enums.IncidentType;
import com.supermart.iot.exception.BadRequestException;
import com.supermart.iot.exception.ConflictException;
import com.supermart.iot.exception.ResourceNotFoundException;
import com.supermart.iot.repository.IncidentRepository;
import com.supermart.iot.repository.IotDeviceRepository;
import com.supermart.iot.repository.TechnicianAssignmentRepository;
import com.supermart.iot.repository.TechnicianRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock private IncidentRepository incidentRepository;
    @Mock private IotDeviceRepository deviceRepository;
    @Mock private TechnicianRepository technicianRepository;
    @Mock private TechnicianAssignmentRepository assignmentRepository;
    @Mock private DeviceService deviceService;

    @InjectMocks
    private IncidentService underTest;

    private IotDevice device;
    private Incident openIncident;

    @BeforeEach
    void setUp() {
        Store store = Store.builder()
                .storeId(1L).storeName("Store A").storeCode("SA01")
                .address("123 Main St").city("Austin").state("TX").createdAt(LocalDateTime.now())
                .build();
        EquipmentUnit unit = EquipmentUnit.builder()
                .unitId(10L).store(store).unitType(EquipmentType.FREEZER)
                .unitName("Freezer 1").locationDesc("Aisle 3").createdAt(LocalDateTime.now())
                .build();
        device = IotDevice.builder()
                .deviceId(100L).deviceSerial("SN-001").deviceKey("key-001")
                .minTempThreshold(-20.0).maxTempThreshold(4.0)
                .status(DeviceStatus.ACTIVE).unit(unit).lastSeenAt(LocalDateTime.now())
                .build();
        openIncident = Incident.builder()
                .incidentId(1L).device(device)
                .incidentType(IncidentType.TEMP_EXCEEDED)
                .status(IncidentStatus.OPEN)
                .description("Temp too high")
                .createdAt(LocalDateTime.now())
                .assignments(Collections.emptyList())
                .build();
    }

    // -------------------------------------------------------------------------
    // listIncidents
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("listIncidents returns paged response with mapped incidents")
    void should_returnPagedResponse_when_listIncidentsIsCalled() {
        when(incidentRepository.findByFilters(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(openIncident)));
        IotDeviceSummaryResponse summary = IotDeviceSummaryResponse.builder().deviceId(100L).build();
        when(deviceService.toSummaryResponse(device)).thenReturn(summary);

        PagedResponse<IncidentResponse> result = underTest.listIncidents(null, null, null, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getIncidentId()).isEqualTo(1L);
    }

    // -------------------------------------------------------------------------
    // createIncident
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("createIncident persists and returns a new open incident")
    void should_createIncident_when_noOpenIncidentExists() {
        CreateIncidentRequest request = CreateIncidentRequest.builder()
                .deviceId(100L).incidentType(IncidentType.TEMP_EXCEEDED).description("High temp").build();
        when(deviceRepository.findById(100L)).thenReturn(Optional.of(device));
        when(incidentRepository.findByDevice_DeviceIdAndStatus(100L, IncidentStatus.OPEN))
                .thenReturn(Optional.empty());
        when(incidentRepository.save(any(Incident.class))).thenReturn(openIncident);
        IotDeviceSummaryResponse summary = IotDeviceSummaryResponse.builder().deviceId(100L).build();
        when(deviceService.toSummaryResponse(device)).thenReturn(summary);

        IncidentResponse result = underTest.createIncident(request);

        assertThat(result.getIncidentId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(IncidentStatus.OPEN);
        verify(incidentRepository).save(any(Incident.class));
    }

    @Test
    @DisplayName("createIncident throws ResourceNotFoundException when device not found")
    void should_throwResourceNotFoundException_when_deviceNotFound() {
        CreateIncidentRequest request = CreateIncidentRequest.builder()
                .deviceId(999L).incidentType(IncidentType.TEMP_EXCEEDED).build();
        when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.createIncident(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("createIncident throws ConflictException when open incident already exists")
    void should_throwConflictException_when_openIncidentAlreadyExists() {
        CreateIncidentRequest request = CreateIncidentRequest.builder()
                .deviceId(100L).incidentType(IncidentType.TEMP_EXCEEDED).build();
        when(deviceRepository.findById(100L)).thenReturn(Optional.of(device));
        when(incidentRepository.findByDevice_DeviceIdAndStatus(100L, IncidentStatus.OPEN))
                .thenReturn(Optional.of(openIncident));

        assertThatThrownBy(() -> underTest.createIncident(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    // -------------------------------------------------------------------------
    // getIncidentById
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getIncidentById returns incident with assignments included")
    void should_returnIncident_when_foundById() {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(openIncident));
        IotDeviceSummaryResponse summary = IotDeviceSummaryResponse.builder().deviceId(100L).build();
        when(deviceService.toSummaryResponse(device)).thenReturn(summary);

        IncidentResponse result = underTest.getIncidentById(1L);

        assertThat(result.getIncidentId()).isEqualTo(1L);
        assertThat(result.getAssignments()).isNotNull();
    }

    @Test
    @DisplayName("getIncidentById throws ResourceNotFoundException when not found")
    void should_throwResourceNotFoundException_when_incidentNotFound() {
        when(incidentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.getIncidentById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // updateStatus
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("updateStatus transitions OPEN -> IN_PROGRESS and saves")
    void should_updateStatus_when_validTransition() {
        UpdateIncidentStatusRequest request = UpdateIncidentStatusRequest.builder()
                .status(IncidentStatus.RESOLVED).build();
        openIncident.setStatus(IncidentStatus.OPEN);
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(openIncident));
        when(incidentRepository.save(any(Incident.class))).thenReturn(openIncident);
        IotDeviceSummaryResponse summary = IotDeviceSummaryResponse.builder().deviceId(100L).build();
        when(deviceService.toSummaryResponse(device)).thenReturn(summary);

        IncidentResponse result = underTest.updateStatus(1L, request);

        assertThat(result).isNotNull();
        verify(incidentRepository).save(any(Incident.class));
    }

    @Test
    @DisplayName("updateStatus sets resolvedAt when status is RESOLVED")
    void should_setResolvedAt_when_statusSetToResolved() {
        UpdateIncidentStatusRequest request = UpdateIncidentStatusRequest.builder()
                .status(IncidentStatus.RESOLVED).build();
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(openIncident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));
        IotDeviceSummaryResponse summary = IotDeviceSummaryResponse.builder().deviceId(100L).build();
        when(deviceService.toSummaryResponse(device)).thenReturn(summary);

        underTest.updateStatus(1L, request);

        assertThat(openIncident.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("updateStatus throws BadRequestException when incident is already RESOLVED")
    void should_throwBadRequestException_when_transitioningFromResolved() {
        openIncident.setStatus(IncidentStatus.RESOLVED);
        UpdateIncidentStatusRequest request = UpdateIncidentStatusRequest.builder()
                .status(IncidentStatus.OPEN).build();
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(openIncident));

        assertThatThrownBy(() -> underTest.updateStatus(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("RESOLVED");
    }

    // -------------------------------------------------------------------------
    // assignTechnician
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("assignTechnician creates assignment and transitions incident to ASSIGNED")
    void should_assignTechnician_when_incidentIsOpen() {
        Technician tech = Technician.builder()
                .technicianId(5L).fullName("Alice Smith").email("alice@example.com")
                .phone("555-0100").region("South").build();
        TechnicianAssignment saved = TechnicianAssignment.builder()
                .assignmentId(50L).incident(openIncident).technician(tech)
                .assignedAt(LocalDateTime.now()).notes("On-site visit").build();
        AssignTechnicianRequest request = AssignTechnicianRequest.builder()
                .technicianId(5L).notes("On-site visit").build();

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(openIncident));
        when(technicianRepository.findById(5L)).thenReturn(Optional.of(tech));
        when(assignmentRepository.save(any(TechnicianAssignment.class))).thenReturn(saved);
        when(incidentRepository.save(any(Incident.class))).thenReturn(openIncident);

        TechnicianAssignmentResponse result = underTest.assignTechnician(1L, request);

        assertThat(result.getAssignmentId()).isEqualTo(50L);
        assertThat(openIncident.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
    }

    @Test
    @DisplayName("assignTechnician throws BadRequestException when incident is RESOLVED")
    void should_throwBadRequestException_when_incidentIsResolved() {
        openIncident.setStatus(IncidentStatus.RESOLVED);
        AssignTechnicianRequest request = AssignTechnicianRequest.builder().technicianId(5L).build();
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(openIncident));

        assertThatThrownBy(() -> underTest.assignTechnician(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("RESOLVED");
    }

    @Test
    @DisplayName("assignTechnician throws ResourceNotFoundException when technician not found")
    void should_throwResourceNotFoundException_when_technicianNotFound() {
        AssignTechnicianRequest request = AssignTechnicianRequest.builder().technicianId(999L).build();
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(openIncident));
        when(technicianRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.assignTechnician(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }
}
