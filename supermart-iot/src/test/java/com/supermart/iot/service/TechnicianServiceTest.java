package com.supermart.iot.service;

import com.supermart.iot.dto.response.PagedResponse;
import com.supermart.iot.dto.response.TechnicianResponse;
import com.supermart.iot.entity.Technician;
import com.supermart.iot.repository.TechnicianRepository;
import com.supermart.iot.service.impl.TechnicianService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TechnicianServiceTest {

    @Mock
    private TechnicianRepository technicianRepository;

    @InjectMocks
    private TechnicianService technicianService;

    private Technician buildTechnician() {
        return Technician.builder()
                .technicianId(1L).fullName("Alice Smith").email("alice@test.com")
                .phone("5551234567").region("North").build();
    }

    @Test
    void listTechnicians_returnsPagedResults() {
        Technician tech = buildTechnician();
        Page<Technician> page = new PageImpl<>(List.of(tech));
        when(technicianRepository.findByRegionAndSearch(any(), any(), any())).thenReturn(page);

        PagedResponse<TechnicianResponse> result = technicianService.listTechnicians(null, null, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTechnicianId()).isEqualTo(1L);
        assertThat(result.getContent().get(0).getFullName()).isEqualTo("Alice Smith");
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("alice@test.com");
    }

    @Test
    void listTechnicians_emptyResult_returnsEmptyPage() {
        when(technicianRepository.findByRegionAndSearch(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        PagedResponse<TechnicianResponse> result = technicianService.listTechnicians("South", "Bob", 0, 10);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void toTechnicianResponse_mapsAllFields() {
        Technician tech = buildTechnician();

        TechnicianResponse response = technicianService.toTechnicianResponse(tech);

        assertThat(response.getTechnicianId()).isEqualTo(1L);
        assertThat(response.getFullName()).isEqualTo("Alice Smith");
        assertThat(response.getEmail()).isEqualTo("alice@test.com");
        assertThat(response.getPhone()).isEqualTo("5551234567");
        assertThat(response.getRegion()).isEqualTo("North");
    }
}
