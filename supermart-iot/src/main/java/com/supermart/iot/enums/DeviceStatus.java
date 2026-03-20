package com.supermart.iot.enums;

/**
 * Operational status values for an IoT device.
 *
 * <p>DECOMMISSIONED is a terminal state set by the soft-delete endpoint
 * (DELETE /devices/{id}). Decommissioned devices are excluded from active
 * dashboard counts and list endpoints by default.</p>
 */
public enum DeviceStatus {
    ACTIVE,
    INACTIVE,
    FAULT,
    /** Device has been permanently retired via the decommission endpoint. */
    DECOMMISSIONED
}
