package com.example.adminwebjava.repository;

import com.example.adminwebjava.entity.TouristBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TouristBookmarkRepository extends JpaRepository {
    List findByTouristId(Integer touristId);

    boolean existsByTouristIdAndPoiId(Integer touristId, Integer poiId);

    void deleteByTouristIdAndPoiId(Integer touristId, Integer poiId);
}