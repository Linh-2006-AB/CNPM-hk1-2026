package com.example.adminwebjava.repository;

import com.example.adminwebjava.entity.PoiReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PoiReviewRepository extends JpaRepository {
    List findByPoiId(Integer poiId);

    List findByTouristId(Integer touristId);
}