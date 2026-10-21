package com.example.adminwebjava.repository;

import com.example.adminwebjava.entity.MediaTask;
import com.example.adminwebjava.entity.MediaTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MediaTaskRepository extends JpaRepository {
    List findByPoiId(Integer poiId);
    List findByStatus(MediaTaskStatus status);
}