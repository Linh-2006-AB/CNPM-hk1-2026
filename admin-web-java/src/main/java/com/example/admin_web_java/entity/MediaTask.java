package com.example.adminwebjava.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "MediaTasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaTask {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "PoiId", insertable = false, updatable = false)
    private Integer poiId;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private MediaTaskType taskType = MediaTaskType.TEXT_TO_AUDIO;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private MediaTaskStatus status = MediaTaskStatus.PENDING;

    @Column(nullable = false)
    private Integer progressPercentage = 0;

    @Column(nullable = false)
    private Integer totalLanguages = 0;

    @Column(nullable = false)
    private Integer succeededLanguages = 0;

    @Column(nullable = false)
    private Integer failedLanguages = 0;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Column(nullable = false)
    private Integer attemptCount = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PoiId")
    private Poi poi;
}