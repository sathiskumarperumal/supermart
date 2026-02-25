package com.supermart.iot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "telemetry_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TelemetryRecord {

    @Id
    @Column(name = "telemetry_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long telemetryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private IotDevice device;

    @Column(nullable = false)
    private Double temperature;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "is_alert", nullable = false)
    private Boolean isAlert;
}
