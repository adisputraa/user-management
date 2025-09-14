package com.bionic.usermanagement.entity.view;

import com.bionic.usermanagement.enums.Role;
import com.bionic.usermanagement.enums.Status;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable // read-only
@Table(name = "v_user_details")
@Getter
public class UserDetailsView {
    @Id
    private UUID id;
    private String username;
    private String email;
    @Enumerated(EnumType.STRING)
    private Role role;
    @Enumerated(EnumType.STRING)
    private Status status;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
    @Column(name = "phone_number")
    private String phoneNumber;
    private String address;
}