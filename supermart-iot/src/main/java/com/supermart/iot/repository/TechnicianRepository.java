package com.supermart.iot.repository;

import com.supermart.iot.entity.Technician;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TechnicianRepository extends JpaRepository<Technician, Long> {

    @Query("SELECT t FROM Technician t WHERE " +
           "(:region IS NULL OR LOWER(t.region) LIKE LOWER(CONCAT('%', :region, '%'))) AND " +
           "(:search IS NULL OR LOWER(t.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(t.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Technician> findByRegionAndSearch(@Param("region") String region,
                                            @Param("search") String search,
                                            Pageable pageable);
}
