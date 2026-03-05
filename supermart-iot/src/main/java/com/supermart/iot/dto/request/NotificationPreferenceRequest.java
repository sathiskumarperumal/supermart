package com.supermart.iot.dto.request;

import com.supermart.iot.enums.NotificationChannel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/**
 * Request DTO for creating or updating a Store Manager's notification preference.
 * Satisfies AC-5: Manager configures Email/SMS/Both preference via profile settings.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationPreferenceRequest {

    /**
     * The notification channel preference.
     * Must be one of: EMAIL, SMS, BOTH.
     */
    @NotNull(message = "channel is required")
    private NotificationChannel channel;

    /**
     * The email address for notification delivery.
     * Required when channel is EMAIL or BOTH.
     */
    @Email(message = "emailAddress must be a valid email address")
    private String emailAddress;

    /**
     * The phone number for SMS delivery in E.164 format (e.g. +14155552671).
     * Required when channel is SMS or BOTH.
     */
    @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "phoneNumber must be in E.164 format (e.g. +14155552671)")
    private String phoneNumber;
}
