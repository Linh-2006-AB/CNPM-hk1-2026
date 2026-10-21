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
    private List reviews = new ArrayList<>();

    @OneToMany(mappedBy = "tourist", cascade = CascadeType.ALL)
    private List favorites = new ArrayList<>();

    @OneToMany(mappedBy = "tourist", cascade = CascadeType.ALL)
    private List playbackLogs = new ArrayList<>();

    @OneToMany(mappedBy = "tourist", cascade = CascadeType.ALL)
    private List poiDiscoveries = new ArrayList<>();

    @OneToMany(mappedBy = "tourist", cascade = CascadeType.ALL)
    private List subscriptions = new ArrayList<>();

    @OneToMany(mappedBy = "tourist", cascade = CascadeType.ALL)
    private List menuOrders = new ArrayList<>();
}