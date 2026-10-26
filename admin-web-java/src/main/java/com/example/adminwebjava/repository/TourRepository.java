package com.example.adminwebjava.repository;

import com.example.adminwebjava.entity.Tour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface TourRepository extends JpaRepository<Tour, Integer> {
    List<Tour> findByStatus(String status);

    @Query("select t from Tour t where not exists (select tr.id from TourTranslation tr where tr.tourId = t.id and tr.languageCode = :languageCode)")
    List<Tour> findToursMissingLanguage(@Param("languageCode") String languageCode);
}