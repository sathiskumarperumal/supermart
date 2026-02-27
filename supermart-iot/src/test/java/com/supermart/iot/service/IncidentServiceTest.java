package com.supermart.iot.service;

import com.supermart.iot.dto.request.AssignTechnicianRequest;
import com.supermart.iot.dto.request.CreateIncidentRequest;
import com.supermart.iot.dto.request.UpdateIncidentStatusRequest;
import com.supermart.iot.dto.response.IncidentResponse;
import com.supermart.iot.dto.response.IotDeviceSummaryResponse;
import com.supermart.iot.dto.response.TechnicianAssignmentResponse;
import com.supermart.iot.dto.response.TechnicianResponse;
import com.supermart.iot.entity.*;
import com.supermart.iot.enums.*;
import com.supermart.iot.exception.BadRequestException;
import com.supermart.iot.exception.ConflictException;
import com.supermart.iot.exception.ResourceNotFoundException;
import com.supermart.iot.repository.IncidentRepository;
import com.supermart.iot.repository.IotDeviceRepository;
import com.supermart.iot.repository.TechnicianAssignmentRepository;
import com.supermart.iot.repository.TechnicianRepository;
import com.supermart.iot.service.impl.DeviceService;
import com.supermart.iot.service.impl.IncidentService;
import com.supermart.iot.service.impl.TechnicianService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
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
    @Mock private TechnicianService technicianService;

    @InjectMocks
    private IncidentService incidentService;

    private IotDevice buildDevice() {
        Store store = Store.builder().storeId(1L).storeName("Store A").build();
        EquipmentUnit unit = EquipmentUnit.builder()
                .unitId(1L).store(store).unitType(EquipmentType.REFRIGERATOR).unitName("Unit A").build();
        return IotDevice.builder()
                .deviceId(10L).deviceSerial("DEV-001").deviceKey("key-001")
                .minTempThreshold(0.0).maxTempThreshold(10.0)
                .status(DeviceStatus.ACTIVE).unit(unit).build();
    }

    private Incident buildOpenIncident(IotDevice device) {
        return Incident.builder()
                .incidentId(1L).device(device)
                .incidentType(IncidentType.TEMP_EXCEEDED)
                .status(IncidentStatus.OPEN)
                .description("Test incident")
                .createdAt(LocalDateTime.now())
                .assignments(Collections.emptyList())
                .build();
    }

    private IotDeviceSummaryResponse buildDeviceSummary() {
        return IotDeviceSummaryResponse.builder()
                .deviceId(10L).deviceSerial("DEV-001").status(DeviceStatus.ACTIVE)
                .storeName("Store A").unitName("Unit A").isAlert(false).build();
    }

    @Test
    void listIncidents_returnsPagedResults() {
        IotDevice device = buildDevice();
        Incident incident = buildOpenIncident(device);
        Page<Incident> page = new PageImpl<>(List.of(incident));
        when(incidentRepository.findByFilters(any(), any(), any(), any())).thenReturn(page);
        when(deviceService.toSummaryResponse(any())).thenReturn(buildDeviceSummary());

        var result = incidentService.listIncidents(null, null, null, 0, 10);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void createIncident_success_returnsIncidentResponse() {
        IotDevice device = buildDevice();
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setDeviceId(10L);
        request.setIncidentType(IncidentType.TEMP_EXCEEDED);
        request.setDescription("High temp");

        Incident saved = buildOpenIncident(device);
        when(deviceRepository.findById(10L)).thenReturn(Optional.of(device));
        when(incidentRepository.findByDevice_DeviceIdAndStatus(10L, IncidentStatus.OPEN))
                .thenReturn(Optional.empty());
        when(incidentRepository.save(any())).thenReturn(saved);
        when(deviceService.toSummaryResponse(any())).thenReturn(buildDeviceSummary());

        IncidentResponse response = incidentService.createIncident(request);

        assertThat(response.getIncidentId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(IncidentStatus.OPEN);
    }

    @Test
    void createIncident_deviceNotFound_throwsResourceNotFoundException() {
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setDeviceId(99L);
        when(deviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.createIncident(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createIncident_openIncidentExists_throwsConflictException() {
        IotDevice device = buildDevice();
        Incident existing = buildOpenIncident(device);
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setDeviceId(10L);
        request.setIncidentType(IncidentType.TEMP_EXCEEDED);

        when(deviceRepository.findById(10L)).thenReturn(Optional.of(device));
        when(incidentRepository.findByDevice_DeviceIdAndStatus(10L, IncidentStatus.OPEN))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> incidentService.createIncident(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("open incident");
    }

    @Test
    void getIncidentById_existingId_returnsIncidentResponse() {
        IotDevice device = buildDevice();
        Incident incident = buildOpenIncident(device);
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(deviceService.toSummaryResponse(any())).thenReturn(buildDeviceSummary());

        IncidentResponse response = incidentService.getIncidentById(1L);

        assertThat(response.getIncidentId()).isEqualTo(1L);
    }

    @Test
    void getIncidentById_notFound_throwsResourceNotFoundException() {
        when(incidentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.getIncidentById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateStatus_toAssigned_updatesStatusSuccessfully() {
        IotDevice device = buildDevice();
        Incident incident = buildOpenIncident(device);
        UpdateIncidentStatusRequest request = new UpdateIncidentStatusRequest();
        request.setStatus(IncidentStatus.ASSIGNED);

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any())).thenReturn(incident);
        when(deviceService.toSummaryResponse(any())).thenReturn(buildDeviceSummary());

        IncidentResponse response = incidentService.updateStatus(1L, request);

        assertThat(response.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
    }

    @Test
    void updateStatus_toResolved_setsResolvedAt() {
        IotDevice device = buildDevice();
        Incident incident = buildOpenIncident(device);
        UpdateIncidentStatusRequest request = new UpdateIncidentStatusRequest();
        request.setStatus(IncidentStatus.RESOLVED);

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(deviceService.toSummaryResponse(any())).thenReturn(buildDeviceSummary());

        incidentService.updateStatus(1L, request);

        assertThat(incident.getResolvedAt()).isNotNull();
    }

    @Test
    void updateStatus_fromResolved_throwsBadRequestException() {
        IotDevice device = buildDevice();
        Incident resolved = Incident.builder()
                .incidentId(1L).device(device).status(IncidentStatus.RESOLVED)
                .incidentType(IncidentType.TEMP_EXCEEDED).createdAt(LocalDateTime.now())
                .assignments(Collections.emptyList()).build();
        UpdateIncidentStatusRequest request = new UpdateIncidentStatusRequest();
        request.setStatus(IncidentStatus.OPEN);

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(resolved));

        assertThatThrownBy(() -> incidentService.updateStatus(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("RESOLVED");
    }

    @Test
    void assignTechnician_success_returnsAssignmentResponse() {
        IotDevice device = buildDevice();
        Incident incident = buildOpenIncident(device);
        Technician technician = Technician.builder()
                .technicianId(5L).fullName("John Tech").email("john@test.com")
                .phone("1234567890").region("North").build();
        TechnicianAssignment assignment = TechnicianAssignment.builder()
                .assignmentId(1L).incident(incident).technician(technician)
                .assignedAt(LocalDateTime.now()).notes("Notes").build();
        TechnicianResponse techResponse = TechnicianResponse.builder()
                .technicianId(5L).fullName("John Tech").email("john@test.com").build();

        AssignTechnicianRequest request = new AssignTechnicianRequest();
        request.setTechnicianId(5L);
        request.setNotes("Notes");

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(technicianRepository.findById(5L)).thenReturn(Optional.of(technician));
        when(assignmentRepository.save(any())).thenReturn(assignment);
        when(incidentRepository.save(any())).thenReturn(incident);
        when(technicianService.toTechnicianResponse(technician)).thenReturn(techResponse);

        TechnicianAssignmentResponse response = incidentService.assignTechnician(1L, request);

        assertThat(response.getAssignmentId()).isEqualTo(1L);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
    }

    @Test
    void assignTechnician_resolvedIncident_throwsBadRequestException() {
        IotDevice device = buildDevice();
        Incident resolved = Incident.builder()
                .incidentId(1L).device(device).status(IncidentStatus.RESOLVED)
                .incidentType(IncidentType.TEMP_EXCEEDED).createdAt(LocalDateTime.now()).build();
        AssignTechnicianRequest request = new AssignTechnicianRequest();
        request.setTechnicianId(5L);

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(resolved));

        assertThatThrownBy(() -> incidentService.assignTechnician(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("RESOLVED");
    }

    @Test
    void assignTechnician_technicianNotFound_throwsResourceNotFoundException() {
        IotDevice device = buildDevice();
        Incident incident = buildOpenIncident(device);
        AssignTechnicianRequest request = new AssignTechnicianRequest();
        request.setTechnicianId(99L);

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(technicianRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.assignTechnician(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}
