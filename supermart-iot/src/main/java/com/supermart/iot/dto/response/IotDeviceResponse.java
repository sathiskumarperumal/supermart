package com.supermart.iot.dto.response;

import com.supermart.iot.enums.DeviceStatus;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IotDeviceResponse {
    private Long deviceId;
    private Long unitId;
    private String deviceSerial;
    private Double minTempThreshold;
    private Double maxTempThreshold;
    private DeviceStatus status;
    private LocalDateTime lastSeenAt;
    private EquipmentUnitResponse unit;
}
