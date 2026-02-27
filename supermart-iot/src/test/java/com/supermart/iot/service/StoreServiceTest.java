package com.supermart.iot.service;

import com.supermart.iot.dto.response.EquipmentUnitResponse;
import com.supermart.iot.dto.response.PagedResponse;
import com.supermart.iot.dto.response.StoreResponse;
import com.supermart.iot.entity.EquipmentUnit;
import com.supermart.iot.entity.Store;
import com.supermart.iot.enums.EquipmentType;
import com.supermart.iot.exception.ResourceNotFoundException;
import com.supermart.iot.repository.EquipmentUnitRepository;
import com.supermart.iot.repository.StoreRepository;
import com.supermart.iot.service.impl.StoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private EquipmentUnitRepository unitRepository;

    @InjectMocks
    private StoreService storeService;

    private Store buildStore() {
        return Store.builder()
                .storeId(1L).storeCode("SM-001").storeName("Supermart North")
                .address("123 Main St").city("Springfield").state("IL")
                .zipCode("62701").createdAt(LocalDateTime.now()).build();
    }

    private EquipmentUnit buildUnit(Store store) {
        return EquipmentUnit.builder()
                .unitId(1L).store(store).unitType(EquipmentType.REFRIGERATOR)
                .unitName("Ref A").locationDesc("Aisle 1").createdAt(LocalDateTime.now()).build();
    }

    @Test
    void listStores_returnsPagedStoreResponses() {
        Store store = buildStore();
        Page<Store> page = new PageImpl<>(List.of(store));
        when(storeRepository.findByStateAndSearch(any(), any(), any())).thenReturn(page);
        when(unitRepository.countByStore_StoreId(1L)).thenReturn(3L);

        PagedResponse<StoreResponse> result = storeService.listStores(null, null, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStoreId()).isEqualTo(1L);
        assertThat(result.getContent().get(0).getStoreName()).isEqualTo("Supermart North");
        assertThat(result.getContent().get(0).getUnitCount()).isEqualTo(3);
    }

    @Test
    void getStoreById_existingId_returnsStoreResponse() {
        Store store = buildStore();
        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(unitRepository.countByStore_StoreId(1L)).thenReturn(2L);

        StoreResponse response = storeService.getStoreById(1L);

        assertThat(response.getStoreId()).isEqualTo(1L);
        assertThat(response.getStoreCode()).isEqualTo("SM-001");
        assertThat(response.getState()).isEqualTo("IL");
        assertThat(response.getUnitCount()).isEqualTo(2);
    }

    @Test
    void getStoreById_notFound_throwsResourceNotFoundException() {
        when(storeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storeService.getStoreById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void listStoreUnits_existingStore_returnsPagedUnits() {
        Store store = buildStore();
        EquipmentUnit unit = buildUnit(store);
        Page<EquipmentUnit> page = new PageImpl<>(List.of(unit));

        when(storeRepository.existsById(1L)).thenReturn(true);
        when(unitRepository.findByStoreIdAndType(any(), any(), any())).thenReturn(page);

        PagedResponse<EquipmentUnitResponse> result = storeService.listStoreUnits(1L, null, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUnitId()).isEqualTo(1L);
        assertThat(result.getContent().get(0).getUnitType()).isEqualTo(EquipmentType.REFRIGERATOR);
    }

    @Test
    void listStoreUnits_storeNotFound_throwsResourceNotFoundException() {
        when(storeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> storeService.listStoreUnits(99L, null, 0, 10))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void listStoreUnits_withTypeFilter_returnsFilteredResults() {
        Store store = buildStore();
        EquipmentUnit unit = buildUnit(store);
        Page<EquipmentUnit> page = new PageImpl<>(List.of(unit));

        when(storeRepository.existsById(1L)).thenReturn(true);
        when(unitRepository.findByStoreIdAndType(eq(1L), eq(EquipmentType.REFRIGERATOR), any()))
                .thenReturn(page);

        PagedResponse<EquipmentUnitResponse> result =
                storeService.listStoreUnits(1L, EquipmentType.REFRIGERATOR, 0, 10);

        assertThat(result.getContent()).hasSize(1);
    }

    private <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
