package com.example.adminwebjava.repository;

import com.example.adminwebjava.entity.Poi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

@Repository
public interface PoiRepository extends JpaRepository<Poi, Integer> {
    List<Poi> findByStatus(String status);

    @Query("select p from Poi p where not exists (select t.id from PoiTranslation t where t.poiId = p.id and t.languageCode = :languageCode)")
    List<Poi> findPoisMissingLanguage(@Param("languageCode") String languageCode);
    List<Poi> findByOwnerProfileId(Integer ownerProfileId);
}
