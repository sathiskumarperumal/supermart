package com.supermart.iot.repository;

import com.supermart.iot.entity.EquipmentUnit;
import com.supermart.iot.enums.EquipmentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipmentUnitRepository extends JpaRepository<EquipmentUnit, Long> {

    @Query("SELECT u FROM EquipmentUnit u WHERE u.store.storeId = :storeId " +
           "AND (:type IS NULL OR u.unitType = :type)")
    Page<EquipmentUnit> findByStoreIdAndType(@Param("storeId") Long storeId,
                                              @Param("type") EquipmentType type,
                                              Pageable pageable);

    long countByStore_StoreId(Long storeId);
}
