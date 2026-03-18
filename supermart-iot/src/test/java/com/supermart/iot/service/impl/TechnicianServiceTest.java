package com.supermart.iot.service.impl;

import com.supermart.iot.dto.response.PagedResponse;
import com.supermart.iot.dto.response.TechnicianResponse;
import com.supermart.iot.entity.Technician;
import com.supermart.iot.repository.TechnicianRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TechnicianService}.
 *
 * <p>Covers technician listing and mapping to response DTOs.</p>
 */
@ExtendWith(MockitoExtension.class)
class TechnicianServiceTest {

    @Mock
    private TechnicianRepository technicianRepository;

    @InjectMocks
    private TechnicianService underTest;

    private Technician technician;

    @BeforeEach
    void setUp() {
        technician = Technician.builder()
                .technicianId(55L)
                .fullName("Carlos Rivera")
                .email("c.rivera@hvac-partners.com")
                .phone("+1-214-555-0192")
                .region("Texas South")
                .build();
    }

    // ─── listTechnicians ──────────────────────────────────────────────────────

    @Test
    @DisplayName("listTechnicians returns paged technician responses when technicians exist")
    void should_return_paged_technicians_when_listTechnicians_called() {
        // given
        Page<Technician> page = new PageImpl<>(List.of(technician));
        when(technicianRepository.findByRegionAndSearch(any(), any(), any(Pageable.class))).thenReturn(page);

        // when
        PagedResponse<TechnicianResponse> result = underTest.listTechnicians("Texas South", null, 0, 20);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFullName()).isEqualTo("Carlos Rivera");
        assertThat(result.getContent().get(0).getTechnicianId()).isEqualTo(55L);
        assertThat(result.getContent().get(0).getRegion()).isEqualTo("Texas South");
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("c.rivera@hvac-partners.com");
        assertThat(result.getContent().get(0).getPhone()).isEqualTo("+1-214-555-0192");
    }

    @Test
    @DisplayName("listTechnicians returns empty paged response when no technicians found")
    void should_return_empty_paged_response_when_no_technicians_found() {
        // given
        Page<Technician> emptyPage = new PageImpl<>(List.of());
        when(technicianRepository.findByRegionAndSearch(any(), any(), any(Pageable.class))).thenReturn(emptyPage);

        // when
        PagedResponse<TechnicianResponse> result = underTest.listTechnicians(null, "nonexistent", 0, 20);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }
}
