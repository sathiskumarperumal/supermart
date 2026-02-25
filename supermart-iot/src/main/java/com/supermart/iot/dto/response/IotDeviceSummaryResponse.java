package com.supermart.iot.dto.response;

import com.supermart.iot.enums.DeviceStatus;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IotDeviceSummaryResponse {
    private Long deviceId;
    private String deviceSerial;
    private DeviceStatus status;
    private LocalDateTime lastSeenAt;
    private String storeName;
    private String unitName;
    private Double latestTemperature;
    private Boolean isAlert;
}
