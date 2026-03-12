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
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TechnicianService}.
 */
@ExtendWith(MockitoExtension.class)
class TechnicianServiceTest {

    @Mock
    private TechnicianRepository technicianRepository;

    @InjectMocks
    private TechnicianService underTest;

    @Test
    @DisplayName("listTechnicians returns paged technician responses")
    void should_returnPagedTechnicians_when_listTechniciansCalled() {
        // given
        Technician technician = Technician.builder()
                .technicianId(1L)
                .fullName("Alice Smith")
                .email("alice@supermart.com")
                .phone("+14155552671")
                .region("West")
                .build();

        when(technicianRepository.findByRegionAndSearch(isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(technician)));

        // when
        PagedResponse<TechnicianResponse> result = underTest.listTechnicians(null, null, 0, 10);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFullName()).isEqualTo("Alice Smith");
        assertThat(result.getContent().get(0).getRegion()).isEqualTo("West");
    }

    @Test
    @DisplayName("listTechnicians returns empty page when no technicians match filter")
    void should_returnEmptyPage_when_noTechniciansMatchFilter() {
        // given
        when(technicianRepository.findByRegionAndSearch(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        // when
        PagedResponse<TechnicianResponse> result = underTest.listTechnicians("East", "Bob", 0, 10);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }
}
