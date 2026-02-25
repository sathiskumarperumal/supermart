package com.supermart.iot.controller;

import com.supermart.iot.dto.response.ApiResponse;
import com.supermart.iot.dto.response.PagedResponse;
import com.supermart.iot.dto.response.TechnicianResponse;
import com.supermart.iot.service.impl.TechnicianService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/technicians")
@RequiredArgsConstructor
@Tag(name = "Technicians", description = "HVAC technician directory")
public class TechnicianController {

    private final TechnicianService technicianService;

    @GetMapping
    @Operation(summary = "List HVAC technicians (paginated)")
    public ResponseEntity<ApiResponse<PagedResponse<TechnicianResponse>>> listTechnicians(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                technicianService.listTechnicians(region, search, page, size)));
    }
}
