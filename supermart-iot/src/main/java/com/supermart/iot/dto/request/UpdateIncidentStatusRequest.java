package com.supermart.iot.dto.request;

import com.supermart.iot.enums.IncidentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateIncidentStatusRequest {

    @NotNull(message = "status is required")
    private IncidentStatus status;
}
