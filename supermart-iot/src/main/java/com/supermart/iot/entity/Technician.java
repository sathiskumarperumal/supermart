package com.supermart.iot.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "technicians")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Technician {

    @Id
    @Column(name = "technician_id")
    private Long technicianId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false)
    private String region;
}
