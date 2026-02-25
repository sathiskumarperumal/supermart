package com.supermart.iot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "technician_assignments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TechnicianAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Long assignmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "technician_id", nullable = false)
    private Technician technician;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
