package com.supermart.iot.repository;

import com.supermart.iot.entity.Incident;
import com.supermart.iot.enums.IncidentStatus;
import com.supermart.iot.enums.IncidentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    @Query("SELECT i FROM Incident i WHERE " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:storeId IS NULL OR i.device.unit.store.storeId = :storeId) AND " +
           "(:type IS NULL OR i.incidentType = :type)")
    Page<Incident> findByFilters(@Param("status") IncidentStatus status,
                                  @Param("storeId") Long storeId,
                                  @Param("type") IncidentType type,
                                  Pageable pageable);

    Optional<Incident> findByDevice_DeviceIdAndStatus(Long deviceId, IncidentStatus status);

    long countByStatus(IncidentStatus status);
}
