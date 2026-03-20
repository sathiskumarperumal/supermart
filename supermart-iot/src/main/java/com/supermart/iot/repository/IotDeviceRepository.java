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

/**
 * JPA repository for {@link IotDevice} entities.
 *
 * <p>Provides standard CRUD operations plus queries that support
 * decommission-aware filtering (AC-2, AC-3) and device-key lookup for
 * the IoT ingestion pipeline.</p>
 */
@Repository
public interface IotDeviceRepository extends JpaRepository<IotDevice, Long> {

    /**
     * Finds a device by its unique device key (used by the telemetry ingestion filter).
     *
     * @param deviceKey the device's shared secret key
     * @return an {@link Optional} containing the matching device, or empty if not found
     */
    Optional<IotDevice> findByDeviceKey(String deviceKey);

    /**
     * Lists devices filtered by store and/or status, excluding DECOMMISSIONED devices
     * when {@code includeDecommissioned} is {@code false} (AC-2, AC-3).
     *
     * @param storeId               optional store filter; null means all stores
     * @param status                optional status filter; null means all statuses
     * @param includeDecommissioned when false, excludes devices with status DECOMMISSIONED
     * @param pageable              pagination parameters
     * @return a page of matching devices
     */
    @Query("SELECT d FROM IotDevice d WHERE " +
           "(:storeId IS NULL OR d.unit.store.storeId = :storeId) AND " +
           "(:status IS NULL OR d.status = :status) AND " +
           "(:includeDecommissioned = true OR d.status <> com.supermart.iot.enums.DeviceStatus.DECOMMISSIONED)")
    Page<IotDevice> findByStoreIdAndStatus(@Param("storeId") Long storeId,
                                            @Param("status") DeviceStatus status,
                                            @Param("includeDecommissioned") boolean includeDecommissioned,
                                            Pageable pageable);

    /**
     * Returns all devices in FAULT status or whose most recent telemetry is an alert,
     * excluding DECOMMISSIONED devices from dashboard alerts (AC-2).
     *
     * <p>JPQL does not support LIMIT inside subqueries (SQL-only syntax).
     * Rewritten using EXISTS + MAX(recordedAt) to find devices whose most recent
     * telemetry is an alert, which is semantically identical to the original intent.</p>
     *
     * @param pageable pagination parameters
     * @return a page of alert devices
     */
    @Query("SELECT d FROM IotDevice d WHERE d.status <> com.supermart.iot.enums.DeviceStatus.DECOMMISSIONED AND " +
           "(d.status = 'FAULT' OR " +
           "EXISTS (SELECT t FROM TelemetryRecord t WHERE t.device = d AND t.isAlert = true " +
           "AND t.recordedAt = (SELECT MAX(t2.recordedAt) FROM TelemetryRecord t2 WHERE t2.device = d)))")
    Page<IotDevice> findAllAlertDevices(Pageable pageable);

    /**
     * Counts devices by status — used for dashboard counters.
     * DECOMMISSIONED devices are counted separately and excluded from ACTIVE/FAULT counts.
     *
     * @param status the status to count
     * @return the number of devices with the given status
     */
    long countByStatus(DeviceStatus status);
}
