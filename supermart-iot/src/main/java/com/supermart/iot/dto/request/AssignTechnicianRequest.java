package com.supermart.iot.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssignTechnicianRequest {

    @NotNull(message = "technicianId is required")
    private Long technicianId;

    private String notes;
}
