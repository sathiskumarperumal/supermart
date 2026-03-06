package com.supermart.iot.service.impl;

import com.supermart.iot.dto.response.PagedResponse;
import com.supermart.iot.dto.response.TechnicianResponse;
import com.supermart.iot.entity.Technician;
import com.supermart.iot.repository.TechnicianRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TechnicianServiceTest {

    @Mock private TechnicianRepository technicianRepository;

    @InjectMocks
    private TechnicianService underTest;

    @Test
    @DisplayName("listTechnicians returns paged technician response with all fields mapped")
    void should_returnPagedTechnicians_when_listTechniciansCalled() {
        Technician tech = Technician.builder()
                .technicianId(1L).fullName("Bob Jones").email("bob@example.com")
                .phone("555-0200").region("North").build();
        when(technicianRepository.findByRegionAndSearch(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(tech)));

        PagedResponse<TechnicianResponse> result = underTest.listTechnicians(null, null, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        TechnicianResponse response = result.getContent().get(0);
        assertThat(response.getTechnicianId()).isEqualTo(1L);
        assertThat(response.getFullName()).isEqualTo("Bob Jones");
        assertThat(response.getEmail()).isEqualTo("bob@example.com");
        assertThat(response.getPhone()).isEqualTo("555-0200");
        assertThat(response.getRegion()).isEqualTo("North");
    }

    @Test
    @DisplayName("listTechnicians returns empty page when no technicians match filter")
    void should_returnEmptyPage_when_noTechniciansMatch() {
        when(technicianRepository.findByRegionAndSearch(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        PagedResponse<TechnicianResponse> result = underTest.listTechnicians("Unknown", "xyz", 0, 10);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }
}
