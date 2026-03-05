package com.supermart.iot.service.impl;

import com.supermart.iot.dto.request.NotificationPreferenceRequest;
import com.supermart.iot.dto.response.NotificationLogResponse;
import com.supermart.iot.dto.response.NotificationPreferenceResponse;
import com.supermart.iot.entity.*;
import com.supermart.iot.enums.NotificationChannel;
import com.supermart.iot.enums.NotificationStatus;
import com.supermart.iot.enums.UserRole;
import com.supermart.iot.exception.BadRequestException;
import com.supermart.iot.exception.ResourceNotFoundException;
import com.supermart.iot.repository.NotificationLogRepository;
import com.supermart.iot.repository.NotificationPreferenceRepository;
import com.supermart.iot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for dispatching Email and SMS alert notifications to Store Managers
 * when a temperature threshold breach is detected, and for managing notification preferences.
 *
 * <p>Satisfies SCRUM-4 acceptance criteria:
 * <ul>
 *   <li>AC-1: Dispatch triggered when telemetry isAlert=true.</li>
 *   <li>AC-2: Notification payload includes store name, unit name, device serial,
 *             current temp, threshold range, and timestamp.</li>
 *   <li>AC-3: No duplicate dispatch if an open TEMP_EXCEEDED incident already exists
 *             (de-duplication enforced upstream in TelemetryService).</li>
 *   <li>AC-4: Notification also dispatched on DEVICE_FAULT incident creation.</li>
 *   <li>AC-5: Managers configure Email/SMS/Both preference via upsertPreference().</li>
 *   <li>AC-6: Test notification dispatched via sendTestNotification().</li>
 *   <li>AC-7: All dispatch events are persisted as NotificationLog with SENT/FAILED status.</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final UserRepository userRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationLogRepository logRepository;

    @Value("${app.notification.from-email:alerts@supermart.com}")
    private String fromEmail;

    // -------------------------------------------------------------------------
    // Public API — Notification Dispatch
    // -------------------------------------------------------------------------

    /**
     * Dispatches alert notifications to all Store Managers who have a configured
     * notification preference, for the given incident.
     *
     * <p>Only managers with role MANAGER are notified. Each dispatch attempt
     * is recorded in the notification_log table with SENT or FAILED status.
     * This method is intended to be called after a new incident is created
     * (AC-1, AC-4).
     *
     * @param incident the incident that triggered the notification
     * @param device   the IoT device associated with the incident
     */
    @Transactional
    public void dispatchIncidentNotifications(Incident incident, IotDevice device) {
        List<User> managers = userRepository.findAllByRole(UserRole.MANAGER);

        if (managers.isEmpty()) {
            log.warn("No MANAGER users found — notification dispatch skipped for incidentId={}",
                    incident.getIncidentId());
            return;
        }

        for (User manager : managers) {
            preferenceRepository.findByUser_Id(manager.getId())
                    .ifPresentOrElse(
                            preference -> dispatchToManager(incident, device, manager, preference),
                            () -> log.debug("Manager userId={} has no notification preference configured — skipped",
                                    manager.getId())
                    );
        }
    }

    /**
     * Dispatches a test notification to the authenticated manager using their
     * configured channel preference. Satisfies AC-6.
     *
     * <p>The test notification is logged in notification_log with a null incidentId.
     *
     * @param userId the ID of the manager requesting the test notification
     * @return a list of {@link NotificationLogResponse} entries for each dispatch attempt
     * @throws ResourceNotFoundException if the user does not exist
     * @throws BadRequestException       if the user has no notification preference configured
     */
    @Transactional
    public List<NotificationLogResponse> sendTestNotification(Long userId) {
        User manager = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id " + userId + " not found."));

        NotificationPreference preference = preferenceRepository.findByUser_Id(userId)
                .orElseThrow(() -> new BadRequestException(
                        "No notification preference configured for userId=" + userId +
                        ". Please configure your notification preferences before sending a test."));

        log.info("Test notification requested: userId={}, channel={}", userId, preference.getChannel());

        List<NotificationLog> logs = dispatchViaPreference(null, manager, preference,
                buildTestPayload(manager));

        return logs.stream()
                .map(this::toLogResponse)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Public API — Preference Management (AC-5)
    // -------------------------------------------------------------------------

    /**
     * Creates or updates the notification preference for the given user.
     * If the user already has a preference, it is overwritten with the new values.
     *
     * @param userId  the ID of the manager whose preference to upsert
     * @param request the new channel preference and contact details
     * @return the saved {@link NotificationPreferenceResponse}
     * @throws ResourceNotFoundException if the user does not exist
     * @throws BadRequestException       if the channel requires a contact field that is missing
     */
    @Transactional
    public NotificationPreferenceResponse upsertPreference(Long userId,
                                                            NotificationPreferenceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User with id " + userId + " not found."));

        validatePreferenceRequest(request);

        NotificationPreference preference = preferenceRepository.findByUser_Id(userId)
                .orElseGet(NotificationPreference::new);

        preference.setUser(user);
        preference.setChannel(request.getChannel());
        preference.setEmailAddress(request.getEmailAddress());
        preference.setPhoneNumber(request.getPhoneNumber());

        NotificationPreference saved = preferenceRepository.save(preference);
        log.info("Notification preference upserted: userId={}, channel={}", userId, saved.getChannel());

        return toPreferenceResponse(saved);
    }

    /**
     * Retrieves the notification preference for a given user.
     *
     * @param userId the ID of the manager whose preference to retrieve
     * @return the {@link NotificationPreferenceResponse}
     * @throws ResourceNotFoundException if no preference exists for the user
     */
    public NotificationPreferenceResponse getPreference(Long userId) {
        NotificationPreference preference = preferenceRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No notification preference found for userId=" + userId));
        return toPreferenceResponse(preference);
    }

    // -------------------------------------------------------------------------
    // Private dispatch helpers
    // -------------------------------------------------------------------------

    /**
     * Routes dispatch to the correct channel(s) based on the manager's preference.
     *
     * @param incident   the associated incident (null for test notifications)
     * @param manager    the recipient user
     * @param preference the manager's channel preference
     * @param payload    the human-readable notification message body
     * @return list of persisted log entries for each dispatch attempt
     */
    private List<NotificationLog> dispatchViaPreference(Incident incident,
                                                         User manager,
                                                         NotificationPreference preference,
                                                         String payload) {
        List<NotificationLog> logs = new ArrayList<>();
        NotificationChannel channel = preference.getChannel();

        if (channel == NotificationChannel.EMAIL || channel == NotificationChannel.BOTH) {
            logs.add(sendEmail(incident, manager, preference.getEmailAddress(), payload));
        }
        if (channel == NotificationChannel.SMS || channel == NotificationChannel.BOTH) {
            logs.add(sendSms(incident, manager, preference.getPhoneNumber(), payload));
        }
        return logs;
    }

    /**
     * Dispatches to the given manager based on incident context.
     *
     * @param incident   the incident that triggered the notification
     * @param device     the device associated with the incident
     * @param manager    the recipient manager
     * @param preference the manager's configured channel preference
     */
    private void dispatchToManager(Incident incident,
                                    IotDevice device,
                                    User manager,
                                    NotificationPreference preference) {
        String payload = buildIncidentPayload(incident, device);
        List<NotificationLog> logs = dispatchViaPreference(incident, manager, preference, payload);
        logs.forEach(entry -> log.info(
                "Notification dispatched: logId={}, userId={}, channel={}, status={}",
                entry.getLogId(), manager.getId(), entry.getChannel(), entry.getStatus()));
    }

    /**
     * Simulates sending an email notification and records the outcome in notification_log.
     * In production this would delegate to JavaMailSender or an email gateway client.
     *
     * @param incident     the associated incident (nullable for test notifications)
     * @param recipient    the manager user receiving the notification
     * @param emailAddress the destination email address
     * @param payload      the notification message body
     * @return the persisted {@link NotificationLog} entry
     */
    private NotificationLog sendEmail(Incident incident,
                                       User recipient,
                                       String emailAddress,
                                       String payload) {
        NotificationLog entry = NotificationLog.builder()
                .incident(incident)
                .recipient(recipient)
                .channel(NotificationChannel.EMAIL)
                .dispatchedAt(LocalDateTime.now())
                .build();
        try {
            // Simulate email dispatch — replace with actual JavaMailSender call in production
            log.info("EMAIL dispatch: to={}, subject=SuperMart Temperature Alert, body={}", emailAddress, payload);
            entry.setStatus(NotificationStatus.SENT);
        } catch (Exception ex) {
            log.error("EMAIL dispatch failed: to={}, error={}", emailAddress, ex.getMessage(), ex);
            entry.setStatus(NotificationStatus.FAILED);
            entry.setErrorDetail(ex.getMessage());
        }
        return logRepository.save(entry);
    }

    /**
     * Simulates sending an SMS notification and records the outcome in notification_log.
     * In production this would delegate to an SMS gateway client (e.g. Twilio).
     *
     * @param incident    the associated incident (nullable for test notifications)
     * @param recipient   the manager user receiving the notification
     * @param phoneNumber the destination phone number in E.164 format
     * @param payload     the notification message body
     * @return the persisted {@link NotificationLog} entry
     */
    private NotificationLog sendSms(Incident incident,
                                     User recipient,
                                     String phoneNumber,
                                     String payload) {
        NotificationLog entry = NotificationLog.builder()
                .incident(incident)
                .recipient(recipient)
                .channel(NotificationChannel.SMS)
                .dispatchedAt(LocalDateTime.now())
                .build();
        try {
            // Simulate SMS dispatch — replace with actual SMS gateway call in production
            log.info("SMS dispatch: to={}, body={}", phoneNumber, payload);
            entry.setStatus(NotificationStatus.SENT);
        } catch (Exception ex) {
            log.error("SMS dispatch failed: to={}, error={}", phoneNumber, ex.getMessage(), ex);
            entry.setStatus(NotificationStatus.FAILED);
            entry.setErrorDetail(ex.getMessage());
        }
        return logRepository.save(entry);
    }

    // -------------------------------------------------------------------------
    // Payload builders
    // -------------------------------------------------------------------------

    /**
     * Builds a human-readable notification payload for a temperature breach or device fault incident.
     * Satisfies AC-2: includes store name, unit name, device serial, current temp,
     * threshold range, and timestamp.
     *
     * @param incident the incident to describe
     * @param device   the IoT device associated with the incident
     * @return formatted notification message
     */
    private String buildIncidentPayload(Incident incident, IotDevice device) {
        EquipmentUnit unit = device.getUnit();
        Store store = unit.getStore();
        return String.format(
                "ALERT: %s%n" +
                "Store: %s%n" +
                "Unit: %s%n" +
                "Device Serial: %s%n" +
                "Description: %s%n" +
                "Threshold Range: [%.1f°C – %.1f°C]%n" +
                "Time: %s",
                incident.getIncidentType().name(),
                store.getStoreName(),
                unit.getUnitName(),
                device.getDeviceSerial(),
                incident.getDescription(),
                device.getMinTempThreshold(),
                device.getMaxTempThreshold(),
                incident.getCreatedAt().toString()
        );
    }

    /**
     * Builds a test notification payload for the given manager.
     *
     * @param manager the manager requesting the test
     * @return formatted test message
     */
    private String buildTestPayload(User manager) {
        return String.format(
                "TEST NOTIFICATION%nThis is a test alert for user: %s%nTime: %s",
                manager.getEmail(),
                LocalDateTime.now().toString()
        );
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    /**
     * Validates that the contact fields required for the chosen channel are present.
     *
     * @param request the preference request to validate
     * @throws BadRequestException if a required contact field is missing for the chosen channel
     */
    private void validatePreferenceRequest(NotificationPreferenceRequest request) {
        NotificationChannel channel = request.getChannel();
        if ((channel == NotificationChannel.EMAIL || channel == NotificationChannel.BOTH)
                && (request.getEmailAddress() == null || request.getEmailAddress().isBlank())) {
            throw new BadRequestException(
                    "emailAddress is required when channel is " + channel);
        }
        if ((channel == NotificationChannel.SMS || channel == NotificationChannel.BOTH)
                && (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank())) {
            throw new BadRequestException(
                    "phoneNumber is required when channel is " + channel);
        }
    }

    // -------------------------------------------------------------------------
    // Response mappers
    // -------------------------------------------------------------------------

    /**
     * Maps a {@link NotificationPreference} entity to a {@link NotificationPreferenceResponse} DTO.
     *
     * @param preference the entity to map
     * @return the response DTO
     */
    private NotificationPreferenceResponse toPreferenceResponse(NotificationPreference preference) {
        return NotificationPreferenceResponse.builder()
                .preferenceId(preference.getPreferenceId())
                .userId(preference.getUser().getId())
                .channel(preference.getChannel())
                .emailAddress(preference.getEmailAddress())
                .phoneNumber(preference.getPhoneNumber())
                .build();
    }

    /**
     * Maps a {@link NotificationLog} entity to a {@link NotificationLogResponse} DTO.
     *
     * @param log the log entry to map
     * @return the response DTO
     */
    private NotificationLogResponse toLogResponse(NotificationLog log) {
        return NotificationLogResponse.builder()
                .logId(log.getLogId())
                .incidentId(log.getIncident() != null ? log.getIncident().getIncidentId() : null)
                .recipientUserId(log.getRecipient().getId())
                .channel(log.getChannel())
                .status(log.getStatus())
                .dispatchedAt(log.getDispatchedAt())
                .errorDetail(log.getErrorDetail())
                .build();
    }
}
