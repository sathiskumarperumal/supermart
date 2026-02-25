package com.supermart.iot.entity;

import com.supermart.iot.enums.DeviceStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "iot_devices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IotDevice {

    @Id
    @Column(name = "device_id")
    private Long deviceId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private EquipmentUnit unit;

    @Column(name = "device_serial", nullable = false, unique = true)
    private String deviceSerial;

    @Column(name = "device_key", nullable = false, unique = true)
    private String deviceKey;

    @Column(name = "min_temp_threshold", nullable = false)
    private Double minTempThreshold;

    @Column(name = "max_temp_threshold", nullable = false)
    private Double maxTempThreshold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DeviceStatus status;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @OneToMany(mappedBy = "device", fetch = FetchType.LAZY)
    private List<TelemetryRecord> telemetryRecords;

    @OneToMany(mappedBy = "device", fetch = FetchType.LAZY)
    private List<Incident> incidents;
}
