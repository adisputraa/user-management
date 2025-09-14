package com.bionic.usermanagement.repository;

import com.bionic.usermanagement.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByKeycloakId(String keycloakId);

    @Query(value = "SELECT u FROM User u JOIN FETCH u.userProfile",
            countQuery = "SELECT count(u) FROM User u")
    @Override
    Page<User> findAll(Pageable pageable);
}
