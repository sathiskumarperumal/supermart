package com.supermart.iot.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TelemetryResponse {
    private Long telemetryId;
    private Long deviceId;
    private Double temperature;
    private LocalDateTime recordedAt;
    private Boolean isAlert;
}
