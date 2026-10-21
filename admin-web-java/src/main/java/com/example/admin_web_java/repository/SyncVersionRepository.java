package com.example.adminwebjava.repository;

import com.example.adminwebjava.entity.SyncVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SyncVersionRepository extends JpaRepository {
}