package com.supermart.iot.service.impl;

import com.supermart.iot.dto.response.PagedResponse;
import com.supermart.iot.dto.response.TechnicianResponse;
import com.supermart.iot.entity.Technician;
import com.supermart.iot.repository.TechnicianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TechnicianService {

    private final TechnicianRepository technicianRepository;

    public PagedResponse<TechnicianResponse> listTechnicians(String region, String search, int page, int size) {
        return PagedResponse.of(
                technicianRepository.findByRegionAndSearch(region, search, PageRequest.of(page, size))
                        .map(this::toResponse));
    }

    private TechnicianResponse toResponse(Technician t) {
        return TechnicianResponse.builder()
                .technicianId(t.getTechnicianId())
                .fullName(t.getFullName())
                .email(t.getEmail())
                .phone(t.getPhone())
                .region(t.getRegion())
                .build();
    }
}
