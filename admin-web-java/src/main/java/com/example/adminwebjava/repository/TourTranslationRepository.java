package com.example.adminwebjava.repository;

import com.example.adminwebjava.entity.TourTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TourTranslationRepository extends JpaRepository<TourTranslation, Integer> {
    @Query("select t.tourId from TourTranslation t where t.languageCode = :languageCode")
    List<Integer> findTourIdsByLanguageCode(@Param("languageCode") String languageCode);
}
