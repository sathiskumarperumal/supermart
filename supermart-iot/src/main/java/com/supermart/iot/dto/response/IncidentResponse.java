package com.supermart.iot.dto.response;

import com.supermart.iot.enums.IncidentStatus;
import com.supermart.iot.enums.IncidentType;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IncidentResponse {
    private Long incidentId;
    private Long deviceId;
    private IncidentType incidentType;
    private IncidentStatus status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private IotDeviceSummaryResponse device;
    private List<TechnicianAssignmentResponse> assignments;
}
