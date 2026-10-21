package com.example.adminwebjava.repository;

import com.example.adminwebjava.entity.PoiTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PoiTranslationRepository extends JpaRepository {
    List findByPoiId(Integer poiId);
    Optional findByPoiIdAndLanguageCode(Integer poiId, String languageCode);
}
