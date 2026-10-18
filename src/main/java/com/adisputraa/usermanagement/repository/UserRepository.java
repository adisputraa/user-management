package com.adisputraa.usermanagement.repository;

import com.adisputraa.usermanagement.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Every query fetches the profile in the same SQL statement to avoid one extra query per user (N+1).

    @Query("SELECT u FROM User u JOIN FETCH u.profile WHERE u.id = :id")
    Optional<User> findWithProfileById(@Param("id") UUID id);

    @Query("SELECT u FROM User u JOIN FETCH u.profile WHERE u.keycloakId = :keycloakId")
    Optional<User> findWithProfileByKeycloakId(@Param("keycloakId") String keycloakId);

    @Query(value = "SELECT u FROM User u JOIN FETCH u.profile",
            countQuery = "SELECT count(u) FROM User u")
    Page<User> findAllWithProfile(Pageable pageable);
}
