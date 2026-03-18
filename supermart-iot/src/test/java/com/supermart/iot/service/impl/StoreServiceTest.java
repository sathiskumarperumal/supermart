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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link StoreService}.
 *
 * <p>Covers store listing, store retrieval by ID, unit listing,
 * and resource-not-found error paths.</p>
 */
@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private EquipmentUnitRepository unitRepository;

    @InjectMocks
    private StoreService underTest;

    private Store store;
    private EquipmentUnit unit;

    @BeforeEach
    void setUp() {
        store = Store.builder()
                .storeId(1001L)
                .storeCode("TX-DAL-042")
                .storeName("Supermart Dallas North")
                .address("1234 Commerce Blvd")
                .city("Dallas")
                .state("TX")
                .zipCode("75201")
                .createdAt(LocalDateTime.now())
                .build();

        unit = EquipmentUnit.builder()
                .unitId(501L)
                .store(store)
                .unitType(EquipmentType.FREEZER)
                .unitName("Freezer-Aisle-3")
                .locationDesc("Aisle 3, near dairy section")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ─── listStores ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("listStores returns paged store responses")
    void should_return_paged_stores_when_listStores_called() {
        // given
        Page<Store> page = new PageImpl<>(List.of(store));
        when(storeRepository.findByStateAndSearch(any(), any(), any(Pageable.class))).thenReturn(page);
        when(unitRepository.countByStore_StoreId(1001L)).thenReturn(3L);

        // when
        PagedResponse<StoreResponse> result = underTest.listStores("TX", null, 0, 20);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStoreName()).isEqualTo("Supermart Dallas North");
        assertThat(result.getContent().get(0).getUnitCount()).isEqualTo(3);
    }

    // ─── getStoreById ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getStoreById returns StoreResponse when store found")
    void should_return_store_response_when_store_found_by_id() {
        // given
        when(storeRepository.findById(1001L)).thenReturn(Optional.of(store));
        when(unitRepository.countByStore_StoreId(1001L)).thenReturn(2L);

        // when
        StoreResponse result = underTest.getStoreById(1001L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStoreId()).isEqualTo(1001L);
        assertThat(result.getStoreCode()).isEqualTo("TX-DAL-042");
        assertThat(result.getCity()).isEqualTo("Dallas");
    }

    @Test
    @DisplayName("getStoreById throws ResourceNotFoundException when store not found")
    void should_throw_resource_not_found_when_store_not_found_by_id() {
        // given
        when(storeRepository.findById(9999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> underTest.getStoreById(9999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("9999");
    }

    // ─── listStoreUnits ───────────────────────────────────────────────────────

    @Test
    @DisplayName("listStoreUnits returns paged equipment unit responses when store exists")
    void should_return_paged_unit_responses_when_store_exists() {
        // given
        Page<EquipmentUnit> unitPage = new PageImpl<>(List.of(unit));
        when(storeRepository.existsById(1001L)).thenReturn(true);
        when(unitRepository.findByStoreIdAndType(eq(1001L), any(), any(Pageable.class))).thenReturn(unitPage);

        // when
        PagedResponse<EquipmentUnitResponse> result = underTest.listStoreUnits(1001L, null, 0, 20);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUnitName()).isEqualTo("Freezer-Aisle-3");
        assertThat(result.getContent().get(0).getUnitType()).isEqualTo(EquipmentType.FREEZER);
    }

    @Test
    @DisplayName("listStoreUnits throws ResourceNotFoundException when store does not exist")
    void should_throw_resource_not_found_when_store_not_found_on_listStoreUnits() {
        // given
        when(storeRepository.existsById(9999L)).thenReturn(false);

        // when / then
        assertThatThrownBy(() -> underTest.listStoreUnits(9999L, null, 0, 20))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("9999");
    }
}
