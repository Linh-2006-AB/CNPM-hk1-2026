package com.example.adminwebjava.services;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class TourGuideService {
    private final JdbcTemplate jdbc;
    public TourGuideService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Map<String,Object>> findPois(String lang, String keyword, String status) {
        String sql = "SELECT p.Id AS id, p.Status AS status, COALESCE(pt.Name, '') AS name, COALESCE(pt.ShortDescription, '') AS shortDescription, p.CoverImageUrl AS imageUrl FROM pois p LEFT JOIN poi_translations pt ON pt.PoiId=p.Id AND pt.LanguageCode=? WHERE (?='' OR p.Status=?) AND (?='' OR LOWER(COALESCE(pt.Name,'')) LIKE LOWER(?)) ORDER BY p.Id DESC";
        String k = keyword == null ? "" : keyword.trim();
        return jdbc.queryForList(sql, lang, status == null ? "" : status, status == null ? "" : status, k, "%"+k+"%");
    }
    public Map<String,Object> findPoi(long id, String lang) {
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT p.*," +
                " COALESCE(pt.Name, vt.Name, '(Chưa có tên địa điểm)') AS name," +
                " COALESCE(pt.ShortDescription, vt.ShortDescription, '') AS shortDescription," +
                " COALESCE(pt.FullDescription, vt.FullDescription, '') AS fullDescription," +
                " COALESCE(pt.AudioUrl, vt.AudioUrl) AS audioUrl," +
                " COALESCE(pt.VideoUrl, vt.VideoUrl) AS videoUrl" +
                " FROM pois p" +
                " LEFT JOIN poi_translations pt ON pt.PoiId=p.Id AND lower(pt.LanguageCode)=lower(?)" +
                " LEFT JOIN poi_translations vt ON vt.PoiId=p.Id AND lower(vt.LanguageCode)='vi'" +
                " WHERE p.Id=?", lang, id);
        return rows.isEmpty() ? null : rows.get(0);
    }
    public List<Map<String,Object>> findCategories(String lang) {
        return jdbc.queryForList("SELECT c.Id AS id, COALESCE(ct.Name,'') AS name FROM categories c LEFT JOIN category_translations ct ON ct.CategoryId=c.Id AND ct.LanguageCode=? ORDER BY c.Id", lang);
    }
    public List<Map<String,Object>> findTours(String lang) {
        return jdbc.queryForList("SELECT t.Id AS id, t.Status AS status, COALESCE(tt.Title,'') AS title, COALESCE(tt.Description,'') AS description FROM tours t LEFT JOIN tour_translations tt ON tt.TourId=t.Id AND tt.LanguageCode=? ORDER BY t.Id DESC", lang);
    }
    public Map<String,Object> dashboardStats() {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("pois", count("SELECT COUNT(*) FROM pois")); m.put("tours", count("SELECT COUNT(*) FROM tours"));
        m.put("categories", count("SELECT COUNT(*) FROM categories")); m.put("tourists", count("SELECT COUNT(*) FROM tourists")); return m;
    }
    public List<Map<String,Object>> findAdminLogs() { return List.of(); }
    private long count(String sql) { try { Long n=jdbc.queryForObject(sql, Long.class); return n==null?0:n; } catch (Exception e) { return 0; } }
}
