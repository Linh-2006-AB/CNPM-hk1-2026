package com.example.adminwebjava.repository;

import com.example.adminwebjava.entity.PoiTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface PoiTranslationRepository
        extends JpaRepository<PoiTranslation, Integer> {

    List<PoiTranslation> findByPoiId(Integer poiId);

    @Query("select t.poiId from PoiTranslation t where t.languageCode = :languageCode")
    List<Integer> findPoiIdsByLanguageCode(@Param("languageCode") String languageCode);

    Optional<PoiTranslation> findByPoiIdAndLanguageCode(
            Integer poiId, String languageCode);
}
