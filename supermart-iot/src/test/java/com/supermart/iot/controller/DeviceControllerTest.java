package com.supermart.iot.controller;

import com.supermart.iot.dto.response.*;
import com.supermart.iot.enums.DeviceStatus;
import com.supermart.iot.enums.EquipmentType;
import com.supermart.iot.exception.ResourceNotFoundException;
import com.supermart.iot.repository.IotDeviceRepository;
import com.supermart.iot.repository.UserRepository;
import com.supermart.iot.security.JwtService;
import com.supermart.iot.service.impl.DeviceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for {@link DeviceController}.
 *
 * <p>Uses {@code @WebMvcTest} to test the HTTP layer in isolation.</p>
 */
@WebMvcTest(DeviceController.class)
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeviceService deviceService;

    /** Required by SecurityConfig → DeviceKeyAuthFilter dependency. */
    @MockBean
    private IotDeviceRepository iotDeviceRepository;

    /** Required by SecurityConfig → UserDetailsService dependency. */
    @MockBean
    private UserRepository userRepository;

    /** Required by JwtAuthenticationFilter dependency in security filter chain. */
    @MockBean
    private JwtService jwtService;

    private static final String BASE_URL = "/devices";

    // ─── GET /devices ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /devices returns 200 with paged device list")
    void should_return_200_when_list_devices_called() throws Exception {
        // given
        IotDeviceSummaryResponse summary = IotDeviceSummaryResponse.builder()
                .deviceId(9001L)
                .deviceSerial("DEV-2024-TX-09001")
                .status(DeviceStatus.ACTIVE)
                .storeName("Supermart Dallas")
                .unitName("Freezer-Aisle-3")
                .latestTemperature(-14.8)
                .isAlert(false)
                .lastSeenAt(LocalDateTime.now())
                .build();

        PagedResponse<IotDeviceSummaryResponse> paged = PagedResponse.<IotDeviceSummaryResponse>builder()
                .content(List.of(summary))
                .totalElements(1L)
                .totalPages(1)
                .page(0)
                .size(20)
                .build();

        when(deviceService.listDevices(any(), any(), eq(0), eq(20))).thenReturn(paged);

        // when / then
        mockMvc.perform(get(BASE_URL).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].deviceId").value(9001))
                .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"));
    }

    // ─── GET /devices/{deviceId} ──────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /devices/{deviceId} returns 200 when device found")
    void should_return_200_when_device_found_by_id() throws Exception {
        // given
        EquipmentUnitResponse unitResponse = EquipmentUnitResponse.builder()
                .unitId(501L)
                .storeId(1001L)
                .unitType(EquipmentType.FREEZER)
                .unitName("Freezer-Aisle-3")
                .locationDesc("Aisle 3")
                .createdAt(LocalDateTime.now())
                .build();

        IotDeviceResponse response = IotDeviceResponse.builder()
                .deviceId(9001L)
                .unitId(501L)
                .deviceSerial("DEV-2024-TX-09001")
                .minTempThreshold(-25.0)
                .maxTempThreshold(-15.0)
                .status(DeviceStatus.ACTIVE)
                .lastSeenAt(LocalDateTime.now())
                .unit(unitResponse)
                .build();

        when(deviceService.getDeviceById(9001L)).thenReturn(response);

        // when / then
        mockMvc.perform(get(BASE_URL + "/9001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deviceId").value(9001))
                .andExpect(jsonPath("$.data.deviceSerial").value("DEV-2024-TX-09001"))
                .andExpect(jsonPath("$.data.unit.unitName").value("Freezer-Aisle-3"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /devices/{deviceId} returns 404 when device not found")
    void should_return_404_when_device_not_found_by_id() throws Exception {
        // given
        when(deviceService.getDeviceById(9999L))
                .thenThrow(new ResourceNotFoundException("IoT device with id 9999 not found."));

        // when / then
        mockMvc.perform(get(BASE_URL + "/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    // ─── GET /devices/{deviceId}/telemetry ────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /devices/{deviceId}/telemetry returns 200 with paged telemetry records")
    void should_return_200_when_telemetry_fetched_for_device() throws Exception {
        // given
        TelemetryResponse telemetry = TelemetryResponse.builder()
                .telemetryId(78234441L)
                .deviceId(9001L)
                .temperature(-14.8)
                .recordedAt(LocalDateTime.now())
                .isAlert(false)
                .build();

        PagedResponse<TelemetryResponse> paged = PagedResponse.<TelemetryResponse>builder()
                .content(List.of(telemetry))
                .totalElements(1L)
                .totalPages(1)
                .page(0)
                .size(20)
                .build();

        when(deviceService.getDeviceTelemetry(eq(9001L), any(), any(), eq(0), eq(20))).thenReturn(paged);

        // when / then
        mockMvc.perform(get(BASE_URL + "/9001/telemetry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].temperature").value(-14.8))
                .andExpect(jsonPath("$.data.content[0].isAlert").value(false));
    }
}
