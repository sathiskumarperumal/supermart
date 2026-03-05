package com.supermart.iot.service.impl;

import com.supermart.iot.dto.response.PagedResponse;
import com.supermart.iot.dto.response.StoreResponse;
import com.supermart.iot.entity.Store;
import com.supermart.iot.enums.EquipmentType;
import com.supermart.iot.exception.ResourceNotFoundException;
import com.supermart.iot.repository.EquipmentUnitRepository;
import com.supermart.iot.repository.StoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link StoreService}.
 */
@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private EquipmentUnitRepository unitRepository;

    @InjectMocks
    private StoreService underTest;

    @Test
    @DisplayName("listStores returns paged response with stores")
    void should_returnPagedStores_when_listStoresCalled() {
        // given
        Store store = Store.builder()
                .storeId(1L)
                .storeCode("SM001")
                .storeName("SuperMart North")
                .address("123 Main St")
                .city("Springfield")
                .state("IL")
                .zipCode("62701")
                .createdAt(LocalDateTime.now())
                .build();

        when(storeRepository.findByStateAndSearch(isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(store)));
        when(unitRepository.countByStore_StoreId(1L)).thenReturn(2L);

        // when
        PagedResponse<StoreResponse> result = underTest.listStores(null, null, 0, 10);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStoreName()).isEqualTo("SuperMart North");
    }

    @Test
    @DisplayName("getStoreById returns store response when store exists")
    void should_returnStoreResponse_when_storeFoundById() {
        // given
        Long storeId = 1L;
        Store store = Store.builder()
                .storeId(storeId)
                .storeCode("SM001")
                .storeName("SuperMart North")
                .address("123 Main St")
                .city("Springfield")
                .state("IL")
                .zipCode("62701")
                .createdAt(LocalDateTime.now())
                .build();

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(unitRepository.countByStore_StoreId(storeId)).thenReturn(3L);

        // when
        StoreResponse result = underTest.getStoreById(storeId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStoreId()).isEqualTo(storeId);
        assertThat(result.getStoreName()).isEqualTo("SuperMart North");
        assertThat(result.getUnitCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("getStoreById throws ResourceNotFoundException when store not found")
    void should_throwResourceNotFoundException_when_storeNotFoundById() {
        // given
        Long storeId = 999L;
        when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> underTest.getStoreById(storeId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("listStoreUnits throws ResourceNotFoundException when store does not exist")
    void should_throwResourceNotFoundException_when_storeNotFoundForListUnits() {
        // given
        Long storeId = 999L;
        when(storeRepository.existsById(storeId)).thenReturn(false);

        // when / then
        assertThatThrownBy(() -> underTest.listStoreUnits(storeId, EquipmentType.FREEZER, 0, 10))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("listStoreUnits returns empty page when store has no units of given type")
    void should_returnEmptyPage_when_noUnitsMatchType() {
        // given
        Long storeId = 1L;
        when(storeRepository.existsById(storeId)).thenReturn(true);
        when(unitRepository.findByStoreIdAndType(eq(storeId), eq(EquipmentType.REFRIGERATOR), any()))
                .thenReturn(new PageImpl<>(List.of()));

        // when
        PagedResponse<?> result = underTest.listStoreUnits(storeId, EquipmentType.REFRIGERATOR, 0, 10);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }
}
