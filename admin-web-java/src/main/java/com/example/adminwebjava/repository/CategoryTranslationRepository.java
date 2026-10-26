package com.example.adminwebjava.repository;

import com.example.adminwebjava.entity.CategoryTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoryTranslationRepository extends JpaRepository<CategoryTranslation, Integer> {
    @Query("select t.categoryId from CategoryTranslation t where t.languageCode = :languageCode")
    List<Integer> findCategoryIdsByLanguageCode(@Param("languageCode") String languageCode);
}
