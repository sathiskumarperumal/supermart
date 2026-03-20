package com.supermart.iot.repository;

import com.supermart.iot.entity.DeviceDecommissionAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for persisting device decommission audit records
 * into the {@code device_threshold_audit} table.
 *
 * <p>Extends {@link JpaRepository} to inherit standard CRUD operations.
 * No custom queries are needed — decommission audit rows are written
 * via {@link JpaRepository#save(Object)}.</p>
 */
@Repository
public interface DeviceDecommissionAuditRepository extends JpaRepository<DeviceDecommissionAudit, Long> {
}
