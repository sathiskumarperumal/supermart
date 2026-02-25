package com.supermart.iot.entity;

import com.supermart.iot.enums.EquipmentType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "equipment_units")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EquipmentUnit {

    @Id
    @Column(name = "unit_id")
    private Long unitId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", nullable = false, length = 20)
    private EquipmentType unitType;

    @Column(name = "unit_name", nullable = false)
    private String unitName;

    @Column(name = "location_desc")
    private String locationDesc;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "unit", fetch = FetchType.LAZY)
    private IotDevice device;
}
