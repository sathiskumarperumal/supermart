package com.supermart.iot.controller;

import com.supermart.iot.dto.request.NotificationPreferenceRequest;
import com.supermart.iot.dto.response.ApiResponse;
import com.supermart.iot.dto.response.NotificationLogResponse;
import com.supermart.iot.dto.response.NotificationPreferenceResponse;
import com.supermart.iot.service.impl.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for notification preferences and test notification dispatch.
 *
 * <p>Exposes the following endpoints from SCRUM-4:
 * <ul>
 *   <li>POST /notifications/test — triggers a test notification for the requesting manager (AC-6)</li>
 *   <li>GET /users/{id}/notification-preferences — retrieves a manager's preference (AC-5)</li>
 *   <li>PUT /users/{id}/notification-preferences — creates or updates a manager's preference (AC-5)</li>
 * </ul>
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification preferences and test dispatch")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Sends a test notification to the requesting manager using their configured
     * channel preference. Returns the dispatch log entries for verification.
     *
     * <p>Satisfies AC-6: 'Test Notification' button exists in Store Manager settings.
     *
     * @param userId the ID of the manager requesting the test
     * @return list of {@link NotificationLogResponse} entries for each dispatch attempt
     */
    @PostMapping("/notifications/test")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Send a test notification to validate the manager's notification configuration")
    public ResponseEntity<ApiResponse<List<NotificationLogResponse>>> sendTestNotification(
            @RequestParam Long userId) {
        log.info("POST /notifications/test: userId={}", userId);
        List<NotificationLogResponse> logs = notificationService.sendTestNotification(userId);
        return ResponseEntity.ok(ApiResponse.ok(logs, "Test notification dispatched successfully."));
    }

    /**
     * Retrieves the notification preference for a given user.
     *
     * <p>Satisfies AC-5: Manager configures Email/SMS/Both preference via profile settings.
     *
     * @param id the user ID whose preference to retrieve
     * @return the {@link NotificationPreferenceResponse}
     */
    @GetMapping("/users/{id}/notification-preferences")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    @Operation(summary = "Get the notification preference for a Store Manager")
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> getPreference(
            @PathVariable Long id) {
        log.info("GET /users/{}/notification-preferences", id);
        NotificationPreferenceResponse response = notificationService.getPreference(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Creates or updates the notification preference for a given user.
     * If a preference already exists, it is overwritten with the new values.
     *
     * <p>Satisfies AC-5: Manager configures Email/SMS/Both preference via profile settings.
     *
     * @param id      the user ID whose preference to upsert
     * @param request the new channel preference and contact details
     * @return the saved {@link NotificationPreferenceResponse}
     */
    @PutMapping("/users/{id}/notification-preferences")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    @Operation(summary = "Create or update the notification preference for a Store Manager")
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> upsertPreference(
            @PathVariable Long id,
            @Valid @RequestBody NotificationPreferenceRequest request) {
        log.info("PUT /users/{}/notification-preferences: channel={}", id, request.getChannel());
        NotificationPreferenceResponse response = notificationService.upsertPreference(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response,
                "Notification preference updated successfully."));
    }
}
