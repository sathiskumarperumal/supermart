package com.supermart.iot.service.impl;

import com.supermart.iot.dto.request.AssignTechnicianRequest;
import com.supermart.iot.dto.request.CreateIncidentRequest;
import com.supermart.iot.dto.request.UpdateIncidentStatusRequest;
import com.supermart.iot.dto.response.IncidentResponse;
import com.supermart.iot.dto.response.IotDeviceSummaryResponse;
import com.supermart.iot.dto.response.PagedResponse;
import com.supermart.iot.dto.response.TechnicianAssignmentResponse;
import com.supermart.iot.entity.Incident;
import com.supermart.iot.entity.IotDevice;
import com.supermart.iot.entity.Technician;
import com.supermart.iot.entity.TechnicianAssignment;
import com.supermart.iot.enums.IncidentStatus;
import com.supermart.iot.enums.IncidentType;
import com.supermart.iot.exception.BadRequestException;
import com.supermart.iot.exception.ConflictException;
import com.supermart.iot.exception.ResourceNotFoundException;
import com.supermart.iot.repository.IncidentRepository;
import com.supermart.iot.repository.IotDeviceRepository;
import com.supermart.iot.repository.TechnicianAssignmentRepository;
import com.supermart.iot.repository.TechnicianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IotDeviceRepository deviceRepository;
    private final TechnicianRepository technicianRepository;
    private final TechnicianAssignmentRepository assignmentRepository;
    private final DeviceService deviceService;
    private final TechnicianService technicianService;

    @Transactional(readOnly = true)
    public PagedResponse<IncidentResponse> listIncidents(IncidentStatus status, Long storeId,
                                                          IncidentType type, int page, int size) {
        Page<Incident> incidentPage = incidentRepository.findByFilters(
                status, storeId, type, PageRequest.of(page, size));
        return PagedResponse.of(incidentPage.map(i -> toResponse(i, false)));
    }

    @Transactional
    public IncidentResponse createIncident(CreateIncidentRequest request) {
        IotDevice device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "IoT device with id " + request.getDeviceId() + " not found."));

        Optional<Incident> existing = incidentRepository.findByDevice_DeviceIdAndStatus(
                device.getDeviceId(), IncidentStatus.OPEN);
        if (existing.isPresent()) {
            throw new ConflictException(
                    "An open incident (id: " + existing.get().getIncidentId() +
                    ") already exists for device " + device.getDeviceId() +
                    ". Resolve it before creating a new one.");
        }

        Incident incident = Incident.builder()
                .device(device)
                .incidentType(request.getIncidentType())
                .status(IncidentStatus.OPEN)
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(incidentRepository.save(incident), false);
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncidentById(Long incidentId) {
        return toResponse(findOrThrow(incidentId), true);
    }

    @Transactional
    public IncidentResponse updateStatus(Long incidentId, UpdateIncidentStatusRequest request) {
        Incident incident = findOrThrow(incidentId);
        validateStatusTransition(incident.getStatus(), request.getStatus());
        incident.setStatus(request.getStatus());
        if (request.getStatus() == IncidentStatus.RESOLVED) {
            incident.setResolvedAt(LocalDateTime.now());
        }
        return toResponse(incidentRepository.save(incident), true);
    }

    @Transactional
    public TechnicianAssignmentResponse assignTechnician(Long incidentId, AssignTechnicianRequest request) {
        Incident incident = findOrThrow(incidentId);
        if (incident.getStatus() == IncidentStatus.RESOLVED) {
            throw new BadRequestException("Cannot assign a technician to a RESOLVED incident.");
        }

        Technician technician = technicianRepository.findById(request.getTechnicianId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Technician with id " + request.getTechnicianId() + " not found."));

        TechnicianAssignment assignment = TechnicianAssignment.builder()
                .incident(incident)
                .technician(technician)
                .assignedAt(LocalDateTime.now())
                .notes(request.getNotes())
                .build();

        TechnicianAssignment saved = assignmentRepository.save(assignment);
        incident.setStatus(IncidentStatus.ASSIGNED);
        incidentRepository.save(incident);

        return toAssignmentResponse(saved, incidentId);
    }

    private void validateStatusTransition(IncidentStatus current, IncidentStatus next) {
        if (current == IncidentStatus.RESOLVED) {
            throw new BadRequestException("Cannot transition from RESOLVED back to " + next + ".");
        }
    }

    private Incident findOrThrow(Long incidentId) {
        return incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Incident with id " + incidentId + " not found."));
    }

    public IncidentResponse toResponse(Incident incident, boolean includeAssignments) {
        List<TechnicianAssignmentResponse> assignments = null;
        if (includeAssignments && incident.getAssignments() != null) {
            assignments = incident.getAssignments().stream()
                    .map(a -> toAssignmentResponse(a, incident.getIncidentId()))
                    .toList();
        }
        IotDeviceSummaryResponse deviceSummary = deviceService.toSummaryResponse(incident.getDevice());
        return IncidentResponse.builder()
                .incidentId(incident.getIncidentId())
                .deviceId(incident.getDevice().getDeviceId())
                .incidentType(incident.getIncidentType())
                .status(incident.getStatus())
                .description(incident.getDescription())
                .createdAt(incident.getCreatedAt())
                .resolvedAt(incident.getResolvedAt())
                .device(deviceSummary)
                .assignments(assignments)
                .build();
    }

    public TechnicianAssignmentResponse toAssignmentResponse(TechnicianAssignment assignment, Long incidentId) {
        return TechnicianAssignmentResponse.builder()
                .assignmentId(assignment.getAssignmentId())
                .incidentId(incidentId)
                .technician(technicianService.toTechnicianResponse(assignment.getTechnician()))
                .assignedAt(assignment.getAssignedAt())
                .notes(assignment.getNotes())
                .build();
    }
}
