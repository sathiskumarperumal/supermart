package com.supermart.iot.controller;

import com.supermart.iot.dto.response.*;
import com.supermart.iot.enums.EquipmentType;
import com.supermart.iot.service.impl.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stores")
@RequiredArgsConstructor
@Tag(name = "Stores", description = "Store information and equipment units")
public class StoreController {

    private final StoreService storeService;

    @GetMapping
    @Operation(summary = "List all stores (paginated)")
    public ResponseEntity<ApiResponse<PagedResponse<StoreResponse>>> listStores(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(storeService.listStores(state, search, page, size)));
    }

    @GetMapping("/{storeId}")
    @Operation(summary = "Get store by ID")
    public ResponseEntity<ApiResponse<StoreResponse>> getStore(@PathVariable Long storeId) {
        return ResponseEntity.ok(ApiResponse.ok(storeService.getStoreById(storeId)));
    }

    @GetMapping("/{storeId}/units")
    @Operation(summary = "List equipment units in a store")
    public ResponseEntity<ApiResponse<PagedResponse<EquipmentUnitResponse>>> listUnits(
            @PathVariable Long storeId,
            @RequestParam(required = false) EquipmentType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(storeService.listStoreUnits(storeId, type, page, size)));
    }
}
