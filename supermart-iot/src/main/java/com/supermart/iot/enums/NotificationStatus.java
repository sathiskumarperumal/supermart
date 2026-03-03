package com.supermart.iot.enums;

/**
 * Represents the dispatch outcome of a notification attempt.
 * Every notification attempt is recorded as either SENT or FAILED in the notification_log table.
 */
public enum NotificationStatus {

    /** Notification was successfully dispatched to the channel provider. */
    SENT,

    /** Notification dispatch failed; details recorded in the log entry. */
    FAILED
}
