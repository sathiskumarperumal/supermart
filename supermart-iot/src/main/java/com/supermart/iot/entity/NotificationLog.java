package com.supermart.iot.entity;

import com.supermart.iot.enums.NotificationChannel;
import com.supermart.iot.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Audit record for every notification dispatch attempt.
 * Every email or SMS send attempt — whether SENT or FAILED — is persisted here,
 * satisfying AC-7: all dispatch events logged with SENT/FAILED status.
 */
@Entity
@Table(name = "notification_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationLog {

    /** Auto-generated surrogate key for the log entry. */
    @Id
    @Column(name = "log_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    /**
     * The incident that triggered this notification.
     * Nullable — will be null for test notifications sent via POST /notifications/test.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    /**
     * The user (Store Manager) who received — or was intended to receive — this notification.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User recipient;

    /**
     * The channel over which dispatch was attempted (EMAIL or SMS).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 10)
    private NotificationChannel channel;

    /**
     * Whether the dispatch succeeded (SENT) or failed (FAILED).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private NotificationStatus status;

    /**
     * The timestamp when the dispatch was attempted.
     */
    @Column(name = "dispatched_at", nullable = false)
    private LocalDateTime dispatchedAt;

    /**
     * Human-readable description of the failure reason, if status is FAILED.
     * Null when status is SENT.
     */
    @Column(name = "error_detail", columnDefinition = "TEXT")
    private String errorDetail;
}
