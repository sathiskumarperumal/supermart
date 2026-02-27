package com.supermart.iot.controller;

import com.supermart.iot.dto.response.*;
import com.supermart.iot.enums.EquipmentType;
import com.supermart.iot.service.impl.StoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreControllerTest {

    @Mock
    private StoreService storeService;

    @InjectMocks
    private StoreController storeController;

    @Test
    void listStores_returnsOkWithPagedResponse() {
        StoreResponse store = StoreResponse.builder()
                .storeId(1L).storeCode("SM-001").storeName("Supermart North")
                .city("Springfield").state("IL").unitCount(3).build();
        PagedResponse<StoreResponse> paged = PagedResponse.<StoreResponse>builder()
                .content(Collections.singletonList(store)).page(0).size(20)
                .totalElements(1).totalPages(1).build();
        when(storeService.listStores(any(), any(), anyInt(), anyInt())).thenReturn(paged);

        ResponseEntity<ApiResponse<PagedResponse<StoreResponse>>> response =
                storeController.listStores(null, null, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getContent()).hasSize(1);
    }

    @Test
    void getStore_returnsOkWithStoreResponse() {
        StoreResponse store = StoreResponse.builder()
                .storeId(1L).storeCode("SM-001").storeName("Supermart North")
                .city("Springfield").state("IL").unitCount(3)
                .createdAt(LocalDateTime.now()).build();
        when(storeService.getStoreById(1L)).thenReturn(store);

        ResponseEntity<ApiResponse<StoreResponse>> response = storeController.getStore(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getStoreName()).isEqualTo("Supermart North");
    }

    @Test
    void listUnits_returnsOkWithPagedUnits() {
        EquipmentUnitResponse unit = EquipmentUnitResponse.builder()
                .unitId(1L).storeId(1L).unitType(EquipmentType.REFRIGERATOR)
                .unitName("Ref A").locationDesc("Aisle 1")
                .createdAt(LocalDateTime.now()).build();
        PagedResponse<EquipmentUnitResponse> paged = PagedResponse.<EquipmentUnitResponse>builder()
                .content(Collections.singletonList(unit)).page(0).size(20)
                .totalElements(1).totalPages(1).build();
        when(storeService.listStoreUnits(eq(1L), any(), anyInt(), anyInt())).thenReturn(paged);

        ResponseEntity<ApiResponse<PagedResponse<EquipmentUnitResponse>>> response =
                storeController.listUnits(1L, null, 0, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getContent()).hasSize(1);
    }

    private <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
