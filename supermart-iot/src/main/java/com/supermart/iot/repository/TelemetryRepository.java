package com.supermart.iot.repository;

import com.supermart.iot.entity.TelemetryRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TelemetryRepository extends JpaRepository<TelemetryRecord, Long> {

    @Query("SELECT t FROM TelemetryRecord t WHERE t.device.deviceId = :deviceId " +
           "AND (:from IS NULL OR t.recordedAt >= :from) " +
           "AND (:to IS NULL OR t.recordedAt <= :to) " +
           "ORDER BY t.recordedAt DESC")
    Page<TelemetryRecord> findByDeviceIdAndDateRange(@Param("deviceId") Long deviceId,
                                                      @Param("from") LocalDateTime from,
                                                      @Param("to") LocalDateTime to,
                                                      Pageable pageable);

    Optional<TelemetryRecord> findTopByDevice_DeviceIdOrderByRecordedAtDesc(Long deviceId);

    long countByRecordedAtAfterAndIsAlertTrue(LocalDateTime since);

    long countByDevice_DeviceIdAndRecordedAtAfter(Long deviceId, LocalDateTime since);
}
