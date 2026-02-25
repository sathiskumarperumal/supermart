package com.supermart.iot.repository;

import com.supermart.iot.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

    Page<Store> findByState(String state, Pageable pageable);

    @Query("SELECT s FROM Store s WHERE " +
           "(:state IS NULL OR s.state = :state) AND " +
           "(:search IS NULL OR LOWER(s.storeName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(s.storeCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Store> findByStateAndSearch(@Param("state") String state,
                                     @Param("search") String search,
                                     Pageable pageable);
}
