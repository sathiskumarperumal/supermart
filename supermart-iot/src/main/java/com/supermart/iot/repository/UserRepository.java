package com.supermart.iot.repository;

import com.supermart.iot.entity.User;
import com.supermart.iot.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer for {@link User} entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their email address.
     *
     * @param email the email address to search for
     * @return an {@link Optional} containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Returns all users that have the given role.
     * Used by the notification service to retrieve all MANAGER users for alert dispatch.
     *
     * @param role the role to filter by
     * @return list of matching users, or an empty list if none found
     */
    List<User> findAllByRole(UserRole role);
}
