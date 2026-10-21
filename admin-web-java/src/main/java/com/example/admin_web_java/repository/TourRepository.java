package com.example.adminwebjava.repository;

import com.example.adminwebjava.entity.Tour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TourRepository extends JpaRepository {
    List findByStatus(String status);
}