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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final EquipmentUnitRepository unitRepository;

    public PagedResponse<StoreResponse> listStores(String state, String search, int page, int size) {
        Page<Store> storePage = storeRepository.findByStateAndSearch(state, search, PageRequest.of(page, size));
        return PagedResponse.of(storePage.map(this::toResponse));
    }

    public StoreResponse getStoreById(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store with id " + storeId + " not found."));
        return toResponse(store);
    }

    public PagedResponse<EquipmentUnitResponse> listStoreUnits(Long storeId, EquipmentType type, int page, int size) {
        if (!storeRepository.existsById(storeId)) {
            throw new ResourceNotFoundException("Store with id " + storeId + " not found.");
        }
        Page<EquipmentUnit> unitPage = unitRepository.findByStoreIdAndType(storeId, type, PageRequest.of(page, size));
        return PagedResponse.of(unitPage.map(this::toUnitResponse));
    }

    private StoreResponse toResponse(Store store) {
        return StoreResponse.builder()
                .storeId(store.getStoreId())
                .storeCode(store.getStoreCode())
                .storeName(store.getStoreName())
                .address(store.getAddress())
                .city(store.getCity())
                .state(store.getState())
                .zipCode(store.getZipCode())
                .unitCount((int) unitRepository.countByStore_StoreId(store.getStoreId()))
                .createdAt(store.getCreatedAt())
                .build();
    }

    private EquipmentUnitResponse toUnitResponse(EquipmentUnit unit) {
        return EquipmentUnitResponse.builder()
                .unitId(unit.getUnitId())
                .storeId(unit.getStore().getStoreId())
                .unitType(unit.getUnitType())
                .unitName(unit.getUnitName())
                .locationDesc(unit.getLocationDesc())
                .createdAt(unit.getCreatedAt())
                .build();
    }
}
