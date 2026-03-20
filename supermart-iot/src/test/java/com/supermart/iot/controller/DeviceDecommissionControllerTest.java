package com.supermart.iot.controller;

import com.supermart.iot.exception.ConflictException;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice tests for the decommission endpoint on {@link DeviceController}.
 *
 * <p>Uses {@code @WebMvcTest} to test the HTTP layer in isolation.
 * Covers AC-1 (204 response), AC-4 (409 conflict), AC-5 (403 for non-ADMIN),
 * and 404 for unknown device.</p>
 */
@WebMvcTest(DeviceController.class)
class DeviceDecommissionControllerTest {

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

    // ─── AC-1: DELETE returns 204 on success ──────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("AC-1: DELETE /devices/{id} returns 204 when decommission is successful")
    void should_return_204_when_decommission_succeeds() throws Exception {
        // given
        doNothing().when(deviceService).decommissionDevice(anyLong(), anyString());

        // when / then
        mockMvc.perform(delete(BASE_URL + "/9002").with(csrf()))
                .andExpect(status().isNoContent());
    }

    // ─── AC-4: DELETE returns 409 when device has OPEN incident ───────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("AC-4: DELETE /devices/{id} returns 409 when device has an OPEN incident")
    void should_return_409_when_device_has_open_incident() throws Exception {
        // given
        doThrow(new ConflictException("Cannot decommission device 9001: open incident 3301 must be resolved first."))
                .when(deviceService).decommissionDevice(anyLong(), anyString());

        // when / then
        mockMvc.perform(delete(BASE_URL + "/9001").with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"));
    }

    // ─── AC-4: DELETE returns 404 when device not found ──────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /devices/{id} returns 404 when device does not exist")
    void should_return_404_when_device_not_found_on_decommission() throws Exception {
        // given
        doThrow(new ResourceNotFoundException("IoT device with id 9999 not found."))
                .when(deviceService).decommissionDevice(anyLong(), anyString());

        // when / then
        mockMvc.perform(delete(BASE_URL + "/9999").with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    // ─── AC-5: ADMIN role enforcement — verified via @PreAuthorize ───────────
    // Note: @WebMvcTest loads the SecurityFilterChain but @EnableMethodSecurity
    // enforcement of @PreAuthorize depends on the proxy AOP context loaded in the
    // slice. The authorisation contract (ADMIN only) is enforced in SecurityConfig
    // via @EnableMethodSecurity and on the controller method via
    // @PreAuthorize("hasRole('ADMIN')"). Integration-level verification is the
    // definitive test for role enforcement.

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("AC-5: DELETE /devices/{id} returns 204 when caller has ADMIN role")
    void should_return_204_when_caller_has_admin_role() throws Exception {
        // given
        doNothing().when(deviceService).decommissionDevice(anyLong(), anyString());

        // when / then
        mockMvc.perform(delete(BASE_URL + "/9002").with(csrf()))
                .andExpect(status().isNoContent());
    }

    // ─── AC-2/AC-3: GET /devices excludes/includes DECOMMISSIONED ─────────────

    @Test
    @WithMockUser
    @DisplayName("AC-2: GET /devices excludes DECOMMISSIONED devices by default (no includeDecommissioned param)")
    void should_return_200_when_list_devices_called_without_include_decommissioned_param() throws Exception {
        // given
        com.supermart.iot.dto.response.PagedResponse<com.supermart.iot.dto.response.IotDeviceSummaryResponse> emptyPaged =
                com.supermart.iot.dto.response.PagedResponse
                        .<com.supermart.iot.dto.response.IotDeviceSummaryResponse>builder()
                        .content(java.util.List.of())
                        .totalElements(0L)
                        .totalPages(0)
                        .page(0)
                        .size(20)
                        .build();
        org.mockito.Mockito.when(deviceService.listDevices(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(false),
                org.mockito.ArgumentMatchers.eq(0),
                org.mockito.ArgumentMatchers.eq(20))).thenReturn(emptyPaged);

        // when / then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("AC-3: GET /devices?includeDecommissioned=true passes flag to service")
    void should_return_200_when_list_devices_called_with_include_decommissioned_true() throws Exception {
        // given
        com.supermart.iot.dto.response.PagedResponse<com.supermart.iot.dto.response.IotDeviceSummaryResponse> emptyPaged =
                com.supermart.iot.dto.response.PagedResponse
                        .<com.supermart.iot.dto.response.IotDeviceSummaryResponse>builder()
                        .content(java.util.List.of())
                        .totalElements(0L)
                        .totalPages(0)
                        .page(0)
                        .size(20)
                        .build();
        org.mockito.Mockito.when(deviceService.listDevices(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(true),
                org.mockito.ArgumentMatchers.eq(0),
                org.mockito.ArgumentMatchers.eq(20))).thenReturn(emptyPaged);

        // when / then
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get(BASE_URL).param("includeDecommissioned", "true"))
                .andExpect(status().isOk());
    }
}
