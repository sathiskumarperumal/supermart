package com.supermart.iot.dto.response;

import com.supermart.iot.enums.NotificationChannel;
import lombok.*;

/**
 * Response DTO representing a Store Manager's notification preference.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationPreferenceResponse {

    /** The preference record ID. */
    private Long preferenceId;

    /** The user ID this preference belongs to. */
    private Long userId;

    /** The configured notification channel (EMAIL, SMS, or BOTH). */
    private NotificationChannel channel;

    /** The email address configured for notification delivery. */
    private String emailAddress;

    /** The phone number configured for SMS delivery. */
    private String phoneNumber;
}
