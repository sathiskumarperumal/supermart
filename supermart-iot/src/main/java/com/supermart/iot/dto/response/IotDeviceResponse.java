package com.supermart.iot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supermart.iot.enums.DeviceStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Full detail response for an IoT device.
 *
 * <p>Includes all device fields plus the associated equipment unit.
 * {@code decommissionedAt} is non-null only for DECOMMISSIONED devices.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IotDeviceResponse {
    private Long deviceId;
    private Long unitId;
    private String deviceSerial;
    private Double minTempThreshold;
    private Double maxTempThreshold;
    private DeviceStatus status;
    private LocalDateTime lastSeenAt;
    /** Timestamp of decommissioning; null for non-decommissioned devices. */
    private LocalDateTime decommissionedAt;
    private EquipmentUnitResponse unit;
}
