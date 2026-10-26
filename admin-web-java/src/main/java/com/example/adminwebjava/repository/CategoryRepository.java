package com.example.adminwebjava.repository;

import com.example.adminwebjava.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    @Query("select c from Category c where not exists (select tr.id from CategoryTranslation tr where tr.categoryId = c.id and tr.languageCode = :languageCode)")
    List<Category> findCategoriesMissingLanguage(@Param("languageCode") String languageCode);
}
