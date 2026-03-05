package com.supermart.iot.repository;

import com.supermart.iot.entity.NotificationLog;
import com.supermart.iot.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for {@link NotificationLog} entities.
 * Supports querying dispatch history by incident or recipient.
 */
@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    /**
     * Returns all notification log entries for a specific incident.
     *
     * @param incidentId the incident whose dispatch history to retrieve
     * @return list of log entries ordered by insertion (natural JPA order)
     */
    List<NotificationLog> findByIncident_IncidentId(Long incidentId);

    /**
     * Returns all notification log entries for a specific user and dispatch status.
     *
     * @param userId the recipient user ID
     * @param status the dispatch outcome to filter by (SENT or FAILED)
     * @return list of matching log entries
     */
    List<NotificationLog> findByRecipient_IdAndStatus(Long userId, NotificationStatus status);
}
