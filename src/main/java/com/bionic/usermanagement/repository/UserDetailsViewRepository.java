package com.bionic.usermanagement.repository;

import com.bionic.usermanagement.entity.view.UserDetailsView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface UserDetailsViewRepository extends JpaRepository<UserDetailsView, UUID> {}