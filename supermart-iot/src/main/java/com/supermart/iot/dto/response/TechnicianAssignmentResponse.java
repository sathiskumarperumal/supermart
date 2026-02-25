package com.supermart.iot.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TechnicianAssignmentResponse {
    private Long assignmentId;
    private Long incidentId;
    private TechnicianResponse technician;
    private LocalDateTime assignedAt;
    private String notes;
}
