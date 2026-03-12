package com.supermart.iot.enums;

/**
 * Represents the notification delivery channel preference for a Store Manager.
 * Managers may opt to receive alerts via EMAIL only, SMS only, or BOTH channels.
 */
public enum NotificationChannel {

    /** Send alerts via email only. */
    EMAIL,

    /** Send alerts via SMS only. */
    SMS,

    /** Send alerts via both email and SMS. */
    BOTH
}
