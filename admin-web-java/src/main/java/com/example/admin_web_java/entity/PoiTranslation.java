package com.example.adminwebjava.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "PoiTranslations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoiTranslation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "PoiId", insertable = false, updatable = false)
    private Integer poiId;

    @Column(nullable = false, length = 10)
    private String languageCode = "vi";

    @Column(nullable = false)
    private String name = "";

    @Column(columnDefinition = "TEXT")
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String fullDescription;

    @Column(columnDefinition = "TEXT")
    private String ttsScript;

    private String audioUrl;
    private String videoUrl;

    @Column(nullable = false)
    private Integer audioDuration = 0;

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PoiId")
    private Poi poi;
}