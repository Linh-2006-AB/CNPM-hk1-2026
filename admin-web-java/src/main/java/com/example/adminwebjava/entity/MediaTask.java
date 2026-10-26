
package com.example.adminwebjava.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "media_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "Id")
    private UUID id;

    @Column(name = "PoiId", insertable = false, updatable = false)
    private Integer poiId;

    @Enumerated(EnumType.STRING)
    @Column(name = "TaskType", nullable = false)
    private MediaTaskType taskType = MediaTaskType.TEXT_TO_AUDIO;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    private MediaTaskStatus status = MediaTaskStatus.PENDING;

    @Column(name = "ProgressPercentage", nullable = false)
    private Integer progressPercentage = 0;

    @Column(name = "TotalLanguages", nullable = false)
    private Integer totalLanguages = 0;

    @Column(name = "SucceededLanguages", nullable = false)
    private Integer succeededLanguages = 0;

    @Column(name = "FailedLanguages", nullable = false)
    private Integer failedLanguages = 0;

    @Column(name = "LastError", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "AttemptCount", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "UpdatedAt", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "StartedAt")
    private LocalDateTime startedAt;

    @Column(name = "CompletedAt")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PoiId", referencedColumnName = "Id")
    private Poi poi;
}
