package com.supermart.iot.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StoreResponse {
    private Long storeId;
    private String storeCode;
    private String storeName;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private int unitCount;
    private LocalDateTime createdAt;
}
