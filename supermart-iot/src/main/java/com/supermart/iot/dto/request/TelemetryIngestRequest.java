package com.supermart.iot.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TelemetryIngestRequest {

    @NotNull(message = "deviceId is required")
    private Long deviceId;

    @NotNull(message = "temperature is required")
    private Double temperature;

    @NotNull(message = "recordedAt is required")
    private LocalDateTime recordedAt;
}
