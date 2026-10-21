package com.example.adminwebjava.repository;

import com.example.adminwebjava.entity.SupportedLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportedLanguageRepository extends JpaRepository {
    List findByIsActiveTrue();
}