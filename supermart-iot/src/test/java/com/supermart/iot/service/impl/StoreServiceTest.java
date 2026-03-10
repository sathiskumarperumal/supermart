package com.supermart.iot.service.impl;

import com.supermart.iot.dto.response.EquipmentUnitResponse;
import com.supermart.iot.dto.response.PagedResponse;
import com.supermart.iot.dto.response.StoreResponse;
import com.supermart.iot.entity.EquipmentUnit;
import com.supermart.iot.entity.Store;
import com.supermart.iot.enums.EquipmentType;
import com.supermart.iot.exception.ResourceNotFoundException;
import com.supermart.iot.repository.EquipmentUnitRepository;
import com.supermart.iot.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock private StoreRepository storeRepository;
    @Mock private EquipmentUnitRepository unitRepository;

    @InjectMocks
    private StoreService underTest;

    private Store store;
    private EquipmentUnit unit;

    @BeforeEach
    void setUp() {
        store = Store.builder()
                .storeId(1L).storeCode("TX01").storeName("Austin Central")
                .address("100 Congress Ave").city("Austin").state("TX")
                .zipCode("78701").createdAt(LocalDateTime.now())
                .build();
        unit = EquipmentUnit.builder()
                .unitId(10L).store(store).unitType(EquipmentType.FREEZER)
                .unitName("Freezer A").locationDesc("Aisle 1").createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("listStores returns paged store response")
    void should_returnPagedStores_when_listStoresCalled() {
        when(storeRepository.findByStateAndSearch(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(store)));
        when(unitRepository.countByStore_StoreId(1L)).thenReturn(3L);

        PagedResponse<StoreResponse> result = underTest.listStores(null, null, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStoreId()).isEqualTo(1L);
        assertThat(result.getContent().get(0).getStoreName()).isEqualTo("Austin Central");
        assertThat(result.getContent().get(0).getUnitCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("getStoreById returns store response when found")
    void should_returnStore_when_foundById() {
        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(unitRepository.countByStore_StoreId(1L)).thenReturn(2L);

        StoreResponse result = underTest.getStoreById(1L);

        assertThat(result.getStoreId()).isEqualTo(1L);
        assertThat(result.getStoreCode()).isEqualTo("TX01");
        assertThat(result.getState()).isEqualTo("TX");
    }

    @Test
    @DisplayName("getStoreById throws ResourceNotFoundException when store not found")
    void should_throwResourceNotFoundException_when_storeNotFound() {
        when(storeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.getStoreById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("listStoreUnits returns paged unit response")
    void should_returnPagedUnits_when_storeExists() {
        when(storeRepository.existsById(1L)).thenReturn(true);
        when(unitRepository.findByStoreIdAndType(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(unit)));

        PagedResponse<EquipmentUnitResponse> result = underTest.listStoreUnits(1L, null, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUnitId()).isEqualTo(10L);
        assertThat(result.getContent().get(0).getUnitType()).isEqualTo(EquipmentType.FREEZER);
    }

    @Test
    @DisplayName("listStoreUnits throws ResourceNotFoundException when store not found")
    void should_throwResourceNotFoundException_when_storeNotFoundForUnits() {
        when(storeRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> underTest.listStoreUnits(999L, null, 0, 10))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }
}
