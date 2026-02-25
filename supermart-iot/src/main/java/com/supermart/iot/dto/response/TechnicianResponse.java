package com.supermart.iot.dto.response;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TechnicianResponse {
    private Long technicianId;
    private String fullName;
    private String email;
    private String phone;
    private String region;
}
