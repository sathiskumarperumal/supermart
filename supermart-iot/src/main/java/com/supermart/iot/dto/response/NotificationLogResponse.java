package com.supermart.iot.dto.response;

import com.supermart.iot.enums.NotificationChannel;
import com.supermart.iot.enums.NotificationStatus;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Response DTO representing a single notification dispatch log entry.
 * Satisfies AC-7: all dispatch events are retrievable with SENT/FAILED status.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationLogResponse {

    /** The log entry ID. */
    private Long logId;

    /** The incident ID that triggered this notification, or null for test notifications. */
    private Long incidentId;

    /** The recipient user ID. */
    private Long recipientUserId;

    /** The channel over which dispatch was attempted. */
    private NotificationChannel channel;

    /** The dispatch outcome (SENT or FAILED). */
    private NotificationStatus status;

    /** The timestamp when the dispatch was attempted. */
    private LocalDateTime dispatchedAt;

    /** Failure reason detail if status is FAILED; null when SENT. */
    private String errorDetail;
}
