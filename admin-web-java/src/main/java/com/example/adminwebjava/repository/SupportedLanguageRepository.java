package com.example.adminwebjava.repository;

import com.example.adminwebjava.entity.SupportedLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SupportedLanguageRepository extends JpaRepository<SupportedLanguage, String> {
    List<SupportedLanguage> findByIsActiveTrueOrderByLanguageCodeAsc();
    List<SupportedLanguage> findByIsActiveTrueAndLanguageCodeNotOrderByLanguageCodeAsc(String languageCode);
    List<SupportedLanguage> findByIsActiveTrueAndLanguageCodeNotAndLanguageCode(String excludedCode, String languageCode);
}
