package com.supermart.iot.repository;

import com.supermart.iot.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access layer for {@link NotificationPreference} entities.
 * Provides lookup by user ID to retrieve a manager's channel preference.
 */
@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    /**
     * Finds the notification preference for a given user ID.
     *
     * @param userId the ID of the user whose preference to retrieve
     * @return an {@link Optional} containing the preference if found, or empty if not configured
     */
    Optional<NotificationPreference> findByUser_Id(Long userId);
}
