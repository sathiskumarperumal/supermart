package com.supermart.iot.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardSummaryResponse {
    private long totalStores;
    private long activeDevices;
    private long faultyDevices;
    private long openIncidents;
    private long alertsLastHour;
    private LocalDateTime asOf;
}
