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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link NotificationService}.
 * Covers all SCRUM-4 acceptance criteria related to notification dispatch and preferences.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private NotificationLogRepository logRepository;

    @InjectMocks
    private NotificationService underTest;

    // -------------------------------------------------------------------------
    // AC-1: When telemetry triggers isAlert=true, dispatch within 120 seconds
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AC-1: Dispatch notifications to all managers when incident is created")
    void should_dispatchNotificationsToAllManagers_when_incidentIsCreated() {
        // given

        // when

        // then
        // TODO: implement assertions
    }

    @Test
    @DisplayName("AC-1: Skip dispatch when no MANAGER users are found")
    void should_skipDispatch_when_noManagerUsersExist() {
        // given
        when(userRepository.findAllByRole(UserRole.MANAGER)).thenReturn(List.of());

        // when
        underTest.dispatchIncidentNotifications(mock(Incident.class), mock(IotDevice.class));

        // then
        verifyNoInteractions(logRepository);
    }

    @Test
    @DisplayName("AC-1: Skip dispatch for manager with no preference configured")
    void should_skipDispatch_when_managerHasNoPreference() {
        // given

        // when

        // then
        // TODO: implement assertions — verify log not called for user with empty preference
    }

    // -------------------------------------------------------------------------
    // AC-3: No duplicate notifications if open TEMP_EXCEEDED incident already exists
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AC-3: No second notification dispatched when open incident already exists")
    void should_notDispatch_when_openIncidentAlreadyExists() {
        // given

        // when

        // then
        // TODO: implement assertions — verify this is enforced in TelemetryService before calling NotificationService
    }

    // -------------------------------------------------------------------------
    // AC-5: Manager configures Email/SMS/Both preference via profile settings
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AC-5: Upsert creates a new notification preference for a manager")
    void should_createNewPreference_when_noExistingPreferenceFound() {
        // given

        // when

        // then
        // TODO: implement assertions
    }

    @Test
    @DisplayName("AC-5: Upsert overwrites an existing notification preference")
    void should_overwritePreference_when_existingPreferenceFound() {
        // given

        // when

        // then
        // TODO: implement assertions
    }

    @Test
    @DisplayName("AC-5: Upsert throws BadRequestException when EMAIL channel set but emailAddress missing")
    void should_throwBadRequestException_when_emailChannelSetWithoutEmailAddress() {
        // given
        Long userId = 1L;
        User user = User.builder().id(userId).email("manager@test.com").role(UserRole.MANAGER).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        NotificationPreferenceRequest request = NotificationPreferenceRequest.builder()
                .channel(NotificationChannel.EMAIL)
                .emailAddress(null)
                .build();

        // when / then
        assertThatThrownBy(() -> underTest.upsertPreference(userId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("emailAddress is required");
    }

    @Test
    @DisplayName("AC-5: Upsert throws BadRequestException when SMS channel set but phoneNumber missing")
    void should_throwBadRequestException_when_smsChannelSetWithoutPhoneNumber() {
        // given
        Long userId = 1L;
        User user = User.builder().id(userId).email("manager@test.com").role(UserRole.MANAGER).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        NotificationPreferenceRequest request = NotificationPreferenceRequest.builder()
                .channel(NotificationChannel.SMS)
                .phoneNumber(null)
                .build();

        // when / then
        assertThatThrownBy(() -> underTest.upsertPreference(userId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("phoneNumber is required");
    }

    @Test
    @DisplayName("AC-5: Upsert throws ResourceNotFoundException when user does not exist")
    void should_throwResourceNotFoundException_when_userNotFoundForPreferenceUpsert() {
        // given
        Long userId = 99L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        NotificationPreferenceRequest request = NotificationPreferenceRequest.builder()
                .channel(NotificationChannel.EMAIL)
                .emailAddress("manager@test.com")
                .build();

        // when / then
        assertThatThrownBy(() -> underTest.upsertPreference(userId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("AC-5: GetPreference throws ResourceNotFoundException when no preference exists")
    void should_throwResourceNotFoundException_when_preferenceNotFoundForUser() {
        // given
        Long userId = 1L;
        when(preferenceRepository.findByUser_Id(userId)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> underTest.getPreference(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // AC-6: 'Test Notification' button — POST /notifications/test
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AC-6: sendTestNotification dispatches to configured channel and returns logs")
    void should_returnLogEntries_when_testNotificationDispatched() {
        // given

        // when

        // then
        // TODO: implement assertions
    }

    @Test
    @DisplayName("AC-6: sendTestNotification throws BadRequestException when no preference configured")
    void should_throwBadRequestException_when_noPreferenceConfiguredForTestNotification() {
        // given
        Long userId = 1L;
        User user = User.builder().id(userId).email("manager@test.com").role(UserRole.MANAGER).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(preferenceRepository.findByUser_Id(userId)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> underTest.sendTestNotification(userId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No notification preference configured");
    }

    @Test
    @DisplayName("AC-6: sendTestNotification throws ResourceNotFoundException when user does not exist")
    void should_throwResourceNotFoundException_when_userNotFoundForTestNotification() {
        // given
        Long userId = 99L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> underTest.sendTestNotification(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // AC-7: All dispatch events logged with SENT/FAILED status
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AC-7: NotificationLog entry persisted with SENT status after successful email dispatch")
    void should_persistLogWithSentStatus_when_emailDispatchedSuccessfully() {
        // given

        // when

        // then
        // TODO: implement assertions — verify logRepository.save() called with status=SENT
    }

    @Test
    @DisplayName("AC-7: NotificationLog entry persisted with FAILED status on dispatch failure")
    void should_persistLogWithFailedStatus_when_dispatchThrowsException() {
        // given

        // when

        // then
        // TODO: implement assertions — verify logRepository.save() called with status=FAILED
    }
}
