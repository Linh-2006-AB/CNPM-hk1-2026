package com.example.adminwebjava.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "owner_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OwnerProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "UserId", nullable = false, unique = true)
    private Integer userId;

    @Column(nullable = false, length = 160)
    private String businessName = "";

    private String representativeName;
    private String phone;
    private String address;

    @Column(nullable = false, length = 30)
    private String status = "Pending";

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId", insertable = false, updatable = false)
    private User user;

    @OneToMany(mappedBy = "ownerProfile")
    private List<Poi> pois = new ArrayList<>();

    @OneToMany(mappedBy = "ownerProfile")
    private List<OwnerSubscription> subscriptions = new ArrayList<>();
}
