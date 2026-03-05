package com.supermart.iot.controller;

import com.supermart.iot.dto.request.NotificationPreferenceRequest;
import com.supermart.iot.dto.response.NotificationLogResponse;
import com.supermart.iot.dto.response.NotificationPreferenceResponse;
import com.supermart.iot.enums.NotificationChannel;
import com.supermart.iot.enums.NotificationStatus;
import com.supermart.iot.exception.BadRequestException;
import com.supermart.iot.service.impl.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link NotificationController}.
 * Covers SCRUM-4 AC-5 (preference management) and AC-6 (test notification).
 */
@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController underTest;

    // -------------------------------------------------------------------------
    // AC-6: POST /notifications/test
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AC-6: POST /notifications/test returns 200 OK with dispatch log entries")
    void should_return200WithLogEntries_when_testNotificationSucceeds() {
        // given
        Long userId = 1L;
        List<NotificationLogResponse> logs = List.of(
                NotificationLogResponse.builder()
                        .logId(1L)
                        .recipientUserId(userId)
                        .channel(NotificationChannel.EMAIL)
                        .status(NotificationStatus.SENT)
                        .dispatchedAt(LocalDateTime.now())
                        .build()
        );
        when(notificationService.sendTestNotification(userId)).thenReturn(logs);

        // when
        ResponseEntity<?> response = underTest.sendTestNotification(userId);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(notificationService).sendTestNotification(userId);
    }

    // -------------------------------------------------------------------------
    // AC-5: GET /users/{id}/notification-preferences
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AC-5: GET /users/{id}/notification-preferences returns 200 with preference")
    void should_return200WithPreference_when_preferenceExistsForUser() {
        // given
        Long userId = 1L;
        NotificationPreferenceResponse preference = NotificationPreferenceResponse.builder()
                .preferenceId(1L)
                .userId(userId)
                .channel(NotificationChannel.BOTH)
                .emailAddress("manager@test.com")
                .phoneNumber("+14155552671")
                .build();
        when(notificationService.getPreference(userId)).thenReturn(preference);

        // when
        ResponseEntity<?> response = underTest.getPreference(userId);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(notificationService).getPreference(userId);
    }

    // -------------------------------------------------------------------------
    // AC-5: PUT /users/{id}/notification-preferences
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AC-5: PUT /users/{id}/notification-preferences returns 200 after upsert")
    void should_return200_when_preferenceUpsertSucceeds() {
        // given
        Long userId = 1L;
        NotificationPreferenceRequest request = NotificationPreferenceRequest.builder()
                .channel(NotificationChannel.EMAIL)
                .emailAddress("manager@test.com")
                .build();
        NotificationPreferenceResponse preference = NotificationPreferenceResponse.builder()
                .preferenceId(1L)
                .userId(userId)
                .channel(NotificationChannel.EMAIL)
                .emailAddress("manager@test.com")
                .build();
        when(notificationService.upsertPreference(userId, request)).thenReturn(preference);

        // when
        ResponseEntity<?> response = underTest.upsertPreference(userId, request);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(notificationService).upsertPreference(userId, request);
    }

    @Test
    @DisplayName("AC-5: PUT /users/{id}/notification-preferences propagates BadRequestException on invalid channel config")
    void should_propagateBadRequestException_when_channelMissingContactDetails() {
        // given
        Long userId = 1L;
        NotificationPreferenceRequest request = NotificationPreferenceRequest.builder()
                .channel(NotificationChannel.SMS)
                .phoneNumber(null)
                .build();
        when(notificationService.upsertPreference(userId, request))
                .thenThrow(new BadRequestException("phoneNumber is required when channel is SMS"));

        // when / then
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> underTest.upsertPreference(userId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("phoneNumber is required when channel is SMS");
        verify(notificationService).upsertPreference(userId, request);
    }
}
