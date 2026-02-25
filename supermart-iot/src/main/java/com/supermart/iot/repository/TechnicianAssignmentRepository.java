package com.supermart.iot.repository;

import com.supermart.iot.entity.TechnicianAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TechnicianAssignmentRepository extends JpaRepository<TechnicianAssignment, Long> {
}
