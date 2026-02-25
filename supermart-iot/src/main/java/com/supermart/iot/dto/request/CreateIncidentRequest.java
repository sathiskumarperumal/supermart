package com.supermart.iot.dto.request;

import com.supermart.iot.enums.IncidentType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateIncidentRequest {

    @NotNull(message = "deviceId is required")
    private Long deviceId;

    @NotNull(message = "incidentType is required")
    private IncidentType incidentType;

    private String description;
}
