package com.example.adminwebjava.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Tourists")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tourist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String email;
    private String passwordHash;
    private String fullName;

    @Column(nullable = false)
    private String authProvider = "local";

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "tourist", cascade = CascadeType.ALL)
    private List<PoiReview> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "tourist", cascade = CascadeType.ALL)
    private List<TouristBookmark> favorites = new ArrayList<>();

    @OneToMany(mappedBy = "tourist", cascade = CascadeType.ALL)
    private List<VisitorPlaybackLog> playbackLogs = new ArrayList<>();

    // Chua co entity PoiDiscovery trong ban Java hien tai.
    @Transient
    private List<Object> poiDiscoveries = new ArrayList<>();

    @OneToMany(mappedBy = "tourist", cascade = CascadeType.ALL)
    private List<TouristSubscription> subscriptions = new ArrayList<>();

    @OneToMany(mappedBy = "tourist", cascade = CascadeType.ALL)
    private List<MenuOrder> menuOrders = new ArrayList<>();
}