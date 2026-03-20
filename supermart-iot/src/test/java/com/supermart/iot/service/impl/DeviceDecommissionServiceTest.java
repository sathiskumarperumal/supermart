package com.supermart.iot.service.impl;

import com.supermart.iot.entity.DeviceDecommissionAudit;
import com.supermart.iot.entity.EquipmentUnit;
import com.supermart.iot.entity.Incident;
import com.supermart.iot.entity.IotDevice;
import com.supermart.iot.entity.Store;
import com.supermart.iot.enums.DeviceStatus;
import com.supermart.iot.enums.EquipmentType;
import com.supermart.iot.enums.IncidentStatus;
import com.supermart.iot.exception.ConflictException;
import com.supermart.iot.exception.ResourceNotFoundException;
import com.supermart.iot.repository.DeviceDecommissionAuditRepository;
import com.supermart.iot.repository.IncidentRepository;
import com.supermart.iot.repository.IotDeviceRepository;
import com.supermart.iot.repository.TelemetryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the decommission functionality in {@link DeviceService}.
 *
 * <p>Covers all acceptance criteria related to device decommissioning:
 * AC-1 (soft-delete), AC-4 (open incident guard), AC-6 (audit log).</p>
 */
@ExtendWith(MockitoExtension.class)
class DeviceDecommissionServiceTest {

    @Mock
    private IotDeviceRepository deviceRepository;

    @Mock
    private TelemetryRepository telemetryRepository;

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private DeviceDecommissionAuditRepository auditRepository;

    @InjectMocks
    private DeviceService underTest;

    private IotDevice device;

    @BeforeEach
    void setUp() {
        Store store = Store.builder()
                .storeId(1001L)
                .storeName("Supermart Dallas")
                .storeCode("TX-DAL-042")
                .build();

        EquipmentUnit unit = EquipmentUnit.builder()
                .unitId(501L)
                .store(store)
                .unitType(EquipmentType.FREEZER)
                .unitName("Freezer-Aisle-3")
                .locationDesc("Aisle 3")
                .createdAt(LocalDateTime.now())
                .build();

        device = IotDevice.builder()
                .deviceId(9002L)
                .unit(unit)
                .deviceSerial("DEV-2024-TX-09002")
                .deviceKey("key-dev-9002")
                .status(DeviceStatus.INACTIVE)
                .minTempThreshold(2.0)
                .maxTempThreshold(8.0)
                .lastSeenAt(LocalDateTime.now())
                .build();
    }

    // ─── AC-1: Soft delete sets status=DECOMMISSIONED, decommissionedAt=now() ──

    @Test
    @DisplayName("AC-1: decommissionDevice sets status DECOMMISSIONED and stamps decommissionedAt")
    void should_setStatusDecommissionedAndStampTimestamp_when_decommissionCalledOnValidDevice() {
        // given
        when(deviceRepository.findById(9002L)).thenReturn(Optional.of(device));
        when(incidentRepository.findByDevice_DeviceIdAndStatus(9002L, IncidentStatus.OPEN))
                .thenReturn(Optional.empty());
        when(deviceRepository.save(any(IotDevice.class))).thenReturn(device);
        when(auditRepository.save(any(DeviceDecommissionAudit.class))).thenReturn(null);

        // when
        underTest.decommissionDevice(9002L, "admin@supermart.com");

        // then
        assertThat(device.getStatus()).isEqualTo(DeviceStatus.DECOMMISSIONED);
        assertThat(device.getDecommissionedAt()).isNotNull();
        verify(deviceRepository).save(device);
    }

    // ─── AC-4: Cannot decommission with OPEN incident ─────────────────────────

    @Test
    @DisplayName("AC-4: decommissionDevice throws ConflictException when device has an OPEN incident")
    void should_throwConflictException_when_deviceHasOpenIncident() {
        // given
        Incident openIncident = Incident.builder()
                .incidentId(3301L)
                .device(device)
                .status(IncidentStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        when(deviceRepository.findById(9002L)).thenReturn(Optional.of(device));
        when(incidentRepository.findByDevice_DeviceIdAndStatus(9002L, IncidentStatus.OPEN))
                .thenReturn(Optional.of(openIncident));

        // when / then
        assertThatThrownBy(() -> underTest.decommissionDevice(9002L, "admin@supermart.com"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("open incident");

        verify(deviceRepository, never()).save(any());
        verify(auditRepository, never()).save(any());
    }

    // ─── AC-4: device not found ─────────────────────────────────────────────

    @Test
    @DisplayName("decommissionDevice throws ResourceNotFoundException when device does not exist")
    void should_throwResourceNotFoundException_when_deviceNotFound() {
        // given
        when(deviceRepository.findById(9999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> underTest.decommissionDevice(9999L, "admin@supermart.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("9999");

        verify(incidentRepository, never()).findByDevice_DeviceIdAndStatus(any(), any());
        verify(deviceRepository, never()).save(any());
    }

    // ─── AC-6: Audit record written to device_threshold_audit ─────────────────

    @Test
    @DisplayName("AC-6: decommissionDevice writes audit record with correct fields")
    void should_writeAuditRecord_when_deviceSuccessfullyDecommissioned() {
        // given
        when(deviceRepository.findById(9002L)).thenReturn(Optional.of(device));
        when(incidentRepository.findByDevice_DeviceIdAndStatus(9002L, IncidentStatus.OPEN))
                .thenReturn(Optional.empty());
        when(deviceRepository.save(any(IotDevice.class))).thenReturn(device);

        ArgumentCaptor<DeviceDecommissionAudit> auditCaptor =
                ArgumentCaptor.forClass(DeviceDecommissionAudit.class);

        // when
        underTest.decommissionDevice(9002L, "admin@supermart.com");

        // then
        verify(auditRepository).save(auditCaptor.capture());
        DeviceDecommissionAudit captured = auditCaptor.getValue();
        assertThat(captured.getDeviceId()).isEqualTo(9002L);
        assertThat(captured.getEventType()).isEqualTo("DECOMMISSIONED");
        assertThat(captured.getPerformedBy()).isEqualTo("admin@supermart.com");
        assertThat(captured.getCreatedAt()).isNotNull();
    }

    // ─── AC-2/AC-3: listDevices excludes DECOMMISSIONED by default ────────────

    @Test
    @DisplayName("AC-2: listDevices excludes DECOMMISSIONED devices by default (includeDecommissioned=false)")
    void should_excludeDecommissionedDevices_when_includeDecommissionedIsFalse() {
        // given — repository mock is verified to receive includeDecommissioned=false
        org.springframework.data.domain.Page<IotDevice> emptyPage =
                new org.springframework.data.domain.PageImpl<>(java.util.List.of());
        when(deviceRepository.findByStoreIdAndStatus(null, null, false,
                org.springframework.data.domain.PageRequest.of(0, 20)))
                .thenReturn(emptyPage);

        // when
        underTest.listDevices(null, null, false, 0, 20);

        // then
        verify(deviceRepository).findByStoreIdAndStatus(null, null, false,
                org.springframework.data.domain.PageRequest.of(0, 20));
    }

    @Test
    @DisplayName("AC-3: listDevices includes DECOMMISSIONED devices when includeDecommissioned=true")
    void should_includeDecommissionedDevices_when_includeDecommissionedIsTrue() {
        // given
        org.springframework.data.domain.Page<IotDevice> emptyPage =
                new org.springframework.data.domain.PageImpl<>(java.util.List.of());
        when(deviceRepository.findByStoreIdAndStatus(null, null, true,
                org.springframework.data.domain.PageRequest.of(0, 20)))
                .thenReturn(emptyPage);

        // when
        underTest.listDevices(null, null, true, 0, 20);

        // then
        verify(deviceRepository).findByStoreIdAndStatus(null, null, true,
                org.springframework.data.domain.PageRequest.of(0, 20));
    }
}
