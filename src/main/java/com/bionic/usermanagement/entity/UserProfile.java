package com.bionic.usermanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "user_profiles")
@Getter @Setter
public class UserProfile {

    @Id
    private UUID id;

    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private User user;
}
