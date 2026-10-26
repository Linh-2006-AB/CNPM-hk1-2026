package com.example.adminwebjava.repository;

import com.example.adminwebjava.entity.MediaTask;
import com.example.adminwebjava.entity.MediaTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaTaskRepository extends JpaRepository<MediaTask, java.util.UUID> {
    List<MediaTask> findByStatusOrderByCreatedAtAsc(MediaTaskStatus status);
    List<MediaTask> findByStatus(MediaTaskStatus status);
    Optional<MediaTask> findFirstByStatusOrderByCreatedAtAsc(MediaTaskStatus status);
}
