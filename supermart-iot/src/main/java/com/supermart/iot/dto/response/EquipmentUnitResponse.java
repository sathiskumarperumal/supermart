package com.supermart.iot.dto.response;

import com.supermart.iot.enums.EquipmentType;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EquipmentUnitResponse {
    private Long unitId;
    private Long storeId;
    private EquipmentType unitType;
    private String unitName;
    private String locationDesc;
    private LocalDateTime createdAt;
}
