package com.supermart.iot.entity;

import com.supermart.iot.enums.NotificationChannel;
import jakarta.persistence.*;
import lombok.*;

/**
 * Stores the notification channel preference for a Store Manager user.
 * Each user has at most one preference record, which controls whether alerts are
 * dispatched via EMAIL, SMS, or BOTH.
 */
@Entity
@Table(name = "notification_preferences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationPreference {

    /** Primary key — matches the user's ID. */
    @Id
    @Column(name = "preference_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long preferenceId;

    /**
     * The manager user who owns this preference.
     * One-to-one relationship; a user may only have one active preference record.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * The channel(s) through which this manager wants to receive alerts.
     * Defaults to BOTH (email and SMS) if not explicitly configured.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 10)
    private NotificationChannel channel;

    /**
     * The email address for notification delivery.
     * May differ from the login email; must be a valid RFC-5321 address.
     */
    @Column(name = "email_address", length = 255)
    private String emailAddress;

    /**
     * The phone number for SMS delivery in E.164 format (e.g. +14155552671).
     */
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
}
