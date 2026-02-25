package com.supermart.iot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "stores")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Store {

    @Id
    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "store_code", nullable = false, unique = true, length = 20)
    private String storeCode;

    @Column(name = "store_name", nullable = false)
    private String storeName;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 2)
    private String state;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "store", fetch = FetchType.LAZY)
    private List<EquipmentUnit> units;

    @Transient
    public int getUnitCount() {
        return units == null ? 0 : units.size();
    }
}
