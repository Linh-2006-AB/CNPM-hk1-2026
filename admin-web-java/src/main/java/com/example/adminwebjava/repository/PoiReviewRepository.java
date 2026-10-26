package com.example.adminwebjava.repository;

import com.example.adminwebjava.entity.PoiReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PoiReviewRepository extends JpaRepository<PoiReview, Integer> {

    List<PoiReview> findByPoiId(Integer poiId);

    List<PoiReview> findByTouristId(Integer touristId);
}
