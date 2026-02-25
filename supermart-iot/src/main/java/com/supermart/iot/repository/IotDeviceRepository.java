package com.supermart.iot.repository;

import com.supermart.iot.entity.IotDevice;
import com.supermart.iot.enums.DeviceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface IotDeviceRepository extends JpaRepository<IotDevice, Long> {

    Optional<IotDevice> findByDeviceKey(String deviceKey);

    @Query("SELECT d FROM IotDevice d WHERE " +
           "(:storeId IS NULL OR d.unit.store.storeId = :storeId) AND " +
           "(:status IS NULL OR d.status = :status)")
    Page<IotDevice> findByStoreIdAndStatus(@Param("storeId") Long storeId,
                                            @Param("status") DeviceStatus status,
                                            Pageable pageable);

    // JPQL does not support LIMIT inside subqueries (SQL-only syntax).
    // Rewritten using EXISTS + MAX(recordedAt) to find devices whose most recent
    // telemetry is an alert, which is semantically identical to the original intent.
    @Query("SELECT d FROM IotDevice d WHERE d.status = 'FAULT' OR " +
           "EXISTS (SELECT t FROM TelemetryRecord t WHERE t.device = d AND t.isAlert = true " +
           "AND t.recordedAt = (SELECT MAX(t2.recordedAt) FROM TelemetryRecord t2 WHERE t2.device = d))")
    Page<IotDevice> findAllAlertDevices(Pageable pageable);

    long countByStatus(DeviceStatus status);
}
