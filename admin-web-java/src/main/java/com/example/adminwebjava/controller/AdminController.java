package com.example.adminwebjava.controller;

import jakarta.servlet.http.HttpServletRequest;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.example.adminwebjava.services.TourGuideService;
import com.example.adminwebjava.services.LanVisitorTrackingService;

/**
 * Consolidated admin endpoints for the Java migration.
 * The original ASP.NET controllers are grouped here intentionally; the SQLite schema is kept unchanged.
 * Web authentication is handled by AuthController with session-based role checks for admin/owner pages.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {
    private final TourGuideService service;
    private final JdbcTemplate jdbc;
    private final LanVisitorTrackingService lanVisitorTrackingService;

    public AdminController(TourGuideService service, JdbcTemplate jdbc, LanVisitorTrackingService lanVisitorTrackingService) {
        this.service = service;
        this.jdbc = jdbc;
        this.lanVisitorTrackingService = lanVisitorTrackingService;
    }

    @GetMapping("/pois/{id}/qr")
        public String showPoiQr(
            @PathVariable long id,
            Model model,
            HttpServletRequest request) {

        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT p.Id AS id,
               COALESCE(pt.Name, vt.Name, 'POI #' || p.Id) AS poiName,
               COALESCE(p.QrCodeToken, '') AS qrCodeToken
            FROM pois p
            LEFT JOIN poi_translations pt
                ON pt.PoiId = p.Id AND lower(pt.LanguageCode) = 'vi'
            LEFT JOIN poi_translations vt
                ON vt.PoiId = p.Id AND lower(vt.LanguageCode) = 'en'
            WHERE p.Id = ?
            LIMIT 1
            """, id);

        if (rows.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Không tìm thấy địa điểm");
        }

        Map<String, Object> poi = rows.get(0);
        String token = String.valueOf(poi.get("qrCodeToken"));

        if (token.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Địa điểm chưa có QR token");
        }

    // Tao URL tu dia chi ma trinh duyet dang dung de mo trang admin.
    // Tu dong lay dia chi host ma trinh duyet dang dung.
    String baseUrl = request.getScheme() + "://"
        + request.getHeader("Host");

    model.addAttribute("poiId", poi.get("id"));
    model.addAttribute("poiName", poi.get("poiName"));
    model.addAttribute("qrCodeToken", token);
    model.addAttribute(
        "qrPayload",
        baseUrl + "/pois/qr/" + token
    );

    return "pois/QRCode";
}


    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("stats", service.dashboardStats());
        model.addAttribute("logs", service.findAdminLogs());
        return "Home/Index";
    }

    @GetMapping("/pois")
    public String pois(@RequestParam(defaultValue = "vi") String lang, Model model) {
        model.addAttribute("pois", service.findPois(lang, "", ""));
        return "pois/Index";
    }

    @GetMapping("/tours")
    public String tours(Model model, @RequestParam(defaultValue = "vi") String lang) {
        model.addAttribute("tours", service.findTours(lang));
        return "Tour/Index";
    }

    // POI administration (the equivalent of the main actions in Poi/Media/translation controllers).
    @ResponseBody
    @GetMapping("/api/pois")
    public List<Map<String, Object>> listPois(@RequestParam(defaultValue = "") String keyword,
                                               @RequestParam(defaultValue = "") String status,
                                               @RequestParam(defaultValue = "vi") String lang) {
        return service.findPois(lang, keyword, status);
    }

    @ResponseBody
    @GetMapping("/api/pois/{id}")
    public Map<String, Object> getPoi(@PathVariable long id,
                                      @RequestParam(defaultValue = "vi") String lang) {
        Map<String, Object> poi = service.findPoi(id, lang);
        if (poi == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy POI " + id);
        return poi;
    }

    @ResponseBody
    @PostMapping("/api/pois/{id}/status")
    public Map<String, Object> updatePoiStatus(@PathVariable long id, @RequestBody Map<String, String> body) {
        String status = required(body, "status");
        int updated = jdbc.update("UPDATE pois SET Status = ? WHERE Id = ?", status, id);
        if (updated == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy POI " + id);
        log("UPDATE", "pois", id, "Cập nhật trạng thái POI thành " + status);
        return Map.of("success", true, "id", id, "status", status);
    }

    @ResponseBody
    @PostMapping("/api/pois/{id}/translations")
    public Map<String, Object> savePoiTranslation(@PathVariable long id, @RequestBody Map<String, String> body) {
        String language = required(body, "languageCode").toLowerCase();
        String name = required(body, "name");
        String shortDescription = body.getOrDefault("shortDescription", "");
        String fullDescription = body.getOrDefault("fullDescription", "");
        String ttsScript = body.getOrDefault("ttsScript", "");
        String audioUrl = body.getOrDefault("audioUrl", "");
        String videoUrl = body.getOrDefault("videoUrl", "");
        int audioDuration = integer(body.get("audioDuration"), 0);
        Integer existing = jdbc.query("SELECT Id FROM poi_translations WHERE PoiId = ? AND lower(LanguageCode) = ? LIMIT 1",
                rs -> rs.next() ? rs.getInt(1) : null, id, language);
        if (existing == null) {
            jdbc.update("INSERT INTO poi_translations(PoiId, LanguageCode, Name, ShortDescription, FullDescription, TtsScript, AudioUrl, AudioDuration, VideoUrl, UpdatedAt) VALUES(?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)",
                    id, language, name, shortDescription, fullDescription, ttsScript, audioUrl, audioDuration, videoUrl);
        } else {
            jdbc.update("UPDATE poi_translations SET Name=?, ShortDescription=?, FullDescription=?, TtsScript=?, AudioUrl=?, AudioDuration=?, VideoUrl=?, UpdatedAt=CURRENT_TIMESTAMP WHERE Id=?",
                    name, shortDescription, fullDescription, ttsScript, audioUrl, audioDuration, videoUrl, existing);
        }
        log("UPSERT", "poi_translations", id, "Lưu bản dịch " + language);
        return Map.of("success", true, "poiId", id, "languageCode", language);
    }

    @ResponseBody
    @GetMapping("/api/pois/{id}/media")
    public List<Map<String, Object>> poiMedia(@PathVariable long id) {
        return jdbc.queryForList("SELECT Id AS id, PoiId AS poiId, MediaType AS mediaType, MediaUrl AS mediaUrl, SortOrder AS sortOrder FROM media_assets WHERE PoiId=? ORDER BY SortOrder, Id", id);
    }

    @ResponseBody
    @PostMapping("/api/pois/{id}/media")
    public Map<String, Object> addPoiMedia(@PathVariable long id, @RequestBody Map<String, Object> body) {
        String mediaType = String.valueOf(body.getOrDefault("mediaType", "image"));
        String mediaUrl = required(body, "mediaUrl");
        int sortOrder = integer(body.get("sortOrder"), 0);
        jdbc.update("INSERT INTO media_assets(PoiId, MediaType, MediaUrl, SortOrder) VALUES(?,?,?,?)", id, mediaType, mediaUrl, sortOrder);
        Long newId = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
        log("INSERT", "media_assets", newId, "Thêm media cho POI " + id);
        return Map.of("success", true, "id", newId == null ? 0 : newId);
    }

    @ResponseBody
    @DeleteMapping("/api/media/{id}")
    public Map<String, Object> deleteMedia(@PathVariable long id) {
        int changed = jdbc.update("DELETE FROM media_assets WHERE Id=?", id);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy media " + id);
        log("DELETE", "media_assets", id, "Xóa media");
        return Map.of("success", true);
    }

    // Tours and tour/POI ordering.
    @ResponseBody
    @GetMapping("/api/tours")
    public List<Map<String, Object>> listTours(@RequestParam(defaultValue = "vi") String lang) {
        return service.findTours(lang);
    }

    @ResponseBody
    @GetMapping("/api/tours/{id}/pois")
    public List<Map<String, Object>> tourPois(@PathVariable long id, @RequestParam(defaultValue = "vi") String lang) {
        return jdbc.queryForList("SELECT tp.TourId AS tourId, tp.PoiId AS poiId, tp.SequenceOrder AS sequenceOrder, COALESCE(pt.Name, '(Chưa có bản dịch)') AS name FROM tour_pois tp LEFT JOIN poi_translations pt ON pt.PoiId=tp.PoiId AND lower(pt.LanguageCode)=? WHERE tp.TourId=? ORDER BY tp.SequenceOrder", lang.toLowerCase(), id);
    }

    @ResponseBody
    @PostMapping("/api/tours/{id}/pois")
    public Map<String, Object> addTourPoi(@PathVariable long id, @RequestBody Map<String, Object> body) {
        long poiId = longValue(body.get("poiId"));
        int sequence = integer(body.get("sequenceOrder"), 0);
        jdbc.update("INSERT INTO tour_pois(TourId, PoiId, SequenceOrder) VALUES(?,?,?) ON CONFLICT(TourId, PoiId) DO UPDATE SET SequenceOrder=excluded.SequenceOrder", id, poiId, sequence);
        log("UPSERT", "tour_pois", poiId, "Gán POI vào tour " + id);
        return Map.of("success", true, "tourId", id, "poiId", poiId, "sequenceOrder", sequence);
    }

    @ResponseBody
    @PutMapping("/api/tours/{id}/pois/order")
    public Map<String, Object> reorderTourPois(@PathVariable long id, @RequestBody Map<String, Object> body) {
        requireExists("tours", id);
        Object raw = body.get("poiIds");
        if (!(raw instanceof List<?> poiIds)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "poiIds phải là một mảng ID POI theo thứ tự mong muốn");
        int sequence = 1;
        for (Object rawId : poiIds) {
            long poiId = longValue(rawId);
            int changed = jdbc.update("UPDATE tour_pois SET SequenceOrder=? WHERE TourId=? AND PoiId=?", sequence++, id, poiId);
            if (changed == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "POI " + poiId + " không thuộc tour " + id);
        }
        log("UPDATE", "tour_pois", id, "Cập nhật thứ tự POI trong tour");
        return Map.of("success", true, "tourId", id, "count", poiIds.size());
    }

    @DeleteMapping("/api/tours/{tourId}/pois/{poiId}")
    public Map<String, Object> removeTourPoi(@PathVariable long tourId, @PathVariable long poiId) {
        jdbc.update("DELETE FROM tour_pois WHERE TourId=? AND PoiId=?", tourId, poiId);
        log("DELETE", "tour_pois", poiId, "Gỡ POI khỏi tour " + tourId);
        return Map.of("success", true);
    }

    // Other controller groups from the C# project are grouped here by admin responsibility.
    @ResponseBody @GetMapping("/api/reviews")
    public List<Map<String, Object>> reviews(@RequestParam(defaultValue = "") String q) {
        return jdbc.queryForList("SELECT r.Id AS id, r.PoiId AS poiId, r.TouristId AS touristId, r.Rating AS rating, r.Comment AS comment, r.CreatedAt AS createdAt FROM poi_reviews r WHERE (?='' OR COALESCE(r.Comment,'') LIKE '%'||?||'%') ORDER BY r.Id DESC LIMIT 500", q, q);
    }

    @ResponseBody @DeleteMapping("/api/reviews/{id}")
    public Map<String, Object> deleteReview(@PathVariable long id) {
        int changed = jdbc.update("DELETE FROM poi_reviews WHERE Id=?", id);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đánh giá " + id);
        log("DELETE", "poi_reviews", id, "Xóa đánh giá");
        return Map.of("success", true);
    }

    @ResponseBody @GetMapping("/api/users")
    public List<Map<String, Object>> users() {
        return jdbc.queryForList("SELECT u.Id AS id, u.Username AS username, u.Email AS email, u.Status AS status, r.RoleName AS role, u.CreatedAt AS createdAt FROM users u LEFT JOIN roles r ON r.Id=u.RoleId ORDER BY u.Id DESC");
    }

    @ResponseBody @PostMapping("/api/users/{id}/lock")
    public Map<String, Object> lockUser(@PathVariable long id) {
        int changed = jdbc.update("UPDATE users SET Status='locked' WHERE Id=?", id);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản " + id);
        log("UPDATE", "users", id, "Khóa tài khoản người dùng");
        return Map.of("success", true, "id", id, "status", "locked");
    }

    @ResponseBody @PostMapping("/api/users/{id}/unlock")
    public Map<String, Object> unlockUser(@PathVariable long id) {
        int changed = jdbc.update("UPDATE users SET Status='active' WHERE Id=?", id);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản " + id);
        log("UPDATE", "users", id, "Mở khóa tài khoản người dùng");
        return Map.of("success", true, "id", id, "status", "active");
    }

    @ResponseBody @GetMapping("/api/tourists")
    public List<Map<String, Object>> tourists(@RequestParam(defaultValue = "") String q) {
        return jdbc.queryForList("SELECT Id AS id, Email AS email, FullName AS fullName, AuthProvider AS authProvider, CreatedAt AS createdAt FROM tourists WHERE (?='' OR Email LIKE '%'||?||'%' OR FullName LIKE '%'||?||'%') ORDER BY Id DESC", q, q, q);
    }

    /** Aggregate overview for the admin dashboard; all counts are read from the existing SQLite schema. */
    @ResponseBody @GetMapping("/api/statistics")
    public Map<String, Object> statistics() {
        long poiTotal = countSql("SELECT COUNT(*) FROM pois");
        long poiApproved = countSql("SELECT COUNT(*) FROM pois WHERE lower(Status)='approved'");
        long tourTotal = countSql("SELECT COUNT(*) FROM tours");
        long userTotal = countSql("SELECT COUNT(*) FROM users");
        long activeUsers = countSql("SELECT COUNT(*) FROM users WHERE lower(Status)='active'");
        long lockedUsers = countSql("SELECT COUNT(*) FROM users WHERE lower(Status) IN ('locked','disabled','blocked')");
        long touristTotal = countSql("SELECT COUNT(*) FROM tourists");
        long reviewTotal = countSql("SELECT COUNT(*) FROM poi_reviews") + countSql("SELECT COUNT(*) FROM reviews");
        long playbackTotal = countSql("SELECT COUNT(*) FROM visitor_playback_logs");
        long discoveryTotal = countSql("SELECT COUNT(*) FROM tourist_poi_discoveries");
        long menuOrderTotal = countSql("SELECT COUNT(*) FROM menu_orders");
        long paymentTotal = countSql("SELECT COUNT(*) FROM payment_transactions");
        return Map.ofEntries(
            Map.entry("poiTotal", poiTotal), Map.entry("poiApproved", poiApproved),
            Map.entry("tourTotal", tourTotal), Map.entry("userTotal", userTotal),
            Map.entry("activeUsers", activeUsers), Map.entry("lockedUsers", lockedUsers),
            Map.entry("touristTotal", touristTotal), Map.entry("reviewTotal", reviewTotal),
            Map.entry("playbackTotal", playbackTotal), Map.entry("discoveryTotal", discoveryTotal),
            Map.entry("menuOrderTotal", menuOrderTotal), Map.entry("paymentTotal", paymentTotal)
        );
    }

    /** Recent audio/playback history for admin diagnostics. days and limit are bounded to avoid unbounded queries. */
    @ResponseBody @GetMapping("/api/audio-history")
    public List<Map<String, Object>> audioHistory(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "200") int limit,
            @RequestParam(required = false) Long poiId,
            @RequestParam(defaultValue = "") String triggerType) {
        int safeDays = Math.max(1, Math.min(days, 365));
        int safeLimit = Math.max(1, Math.min(limit, 1000));
        String trigger = triggerType == null ? "" : triggerType.trim();
        String sql = """
            SELECT l.Id AS id, l.TouristId AS touristId, l.DeviceId AS deviceId,
                   l.PoiId AS poiId, COALESCE(pt.Name, '(Chưa có bản dịch)') AS poiName,
                   l.LanguageCode AS languageCode, l.TriggerType AS triggerType,
                   l.ListenDuration AS listenDuration, l.VisitorLatitude AS visitorLatitude,
                   l.VisitorLongitude AS visitorLongitude, l.CreatedAt AS createdAt
            FROM visitor_playback_logs l
            LEFT JOIN poi_translations pt ON pt.PoiId=l.PoiId AND lower(pt.LanguageCode)='vi'
            WHERE l.CreatedAt >= datetime('now', ?)
              AND (? IS NULL OR l.PoiId=?)
              AND (?='' OR lower(l.TriggerType)=lower(?))
            ORDER BY l.CreatedAt DESC, l.Id DESC LIMIT ?
            """;
        return jdbc.queryForList(sql, "-" + safeDays + " days", poiId, poiId, trigger, trigger, safeLimit);
    }

    @ResponseBody @GetMapping("/api/settings")
    public List<Map<String, Object>> settings() {
        return jdbc.queryForList("SELECT Id AS id, SettingKey AS settingKey, SettingValue AS settingValue, Description AS description FROM system_settings ORDER BY SettingKey");
    }

    @ResponseBody @PostMapping("/api/settings")
    public Map<String, Object> saveSetting(@RequestBody Map<String, String> body) {
        String key = required(body, "settingKey");
        String value = body.getOrDefault("settingValue", "");
        String description = body.getOrDefault("description", "");
        Integer id = jdbc.query("SELECT Id FROM system_settings WHERE SettingKey=? LIMIT 1", rs -> rs.next() ? rs.getInt(1) : null, key);
        if (id == null) jdbc.update("INSERT INTO system_settings(SettingKey, SettingValue, Description) VALUES(?,?,?)", key, value, description);
        else jdbc.update("UPDATE system_settings SET SettingValue=?, Description=? WHERE Id=?", value, description, id);
        log("UPSERT", "system_settings", id, "Lưu cấu hình " + key);
        return Map.of("success", true, "settingKey", key);
    }

    @ResponseBody @GetMapping("/api/payment-plans")
    public List<Map<String, Object>> paymentPlans() {
        return jdbc.queryForList("SELECT Id AS id, PlanCode AS planCode, PlanName AS planName, Audience AS audience, Price AS price, DurationDays AS durationDays, Description AS description, IsActive AS isActive FROM payment_plans ORDER BY Id DESC");
    }

    @ResponseBody @GetMapping("/api/payments")
    public List<Map<String, Object>> payments(@RequestParam(defaultValue = "") String status) {
        return jdbc.queryForList("SELECT Id AS id, TransactionCode AS transactionCode, PayerType AS payerType, Purpose AS purpose, Amount AS amount, Currency AS currency, PaymentMethod AS paymentMethod, Status AS status, CreatedAt AS createdAt, PaidAt AS paidAt FROM payment_transactions WHERE (?='' OR lower(Status)=lower(?)) ORDER BY Id DESC LIMIT 1000", status, status);
    }

    /**
     * Mirrors the original C# PaymentController.Confirm action for the demo/admin API.
     * Marking a payment as paid and activating the related subscription are one transaction.
     */
    @ResponseBody
    @PostMapping("/api/payments/{id}/confirm")
    @Transactional
    public Map<String, Object> confirmPayment(@PathVariable long id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT Id AS id, PayerType AS payerType, OwnerProfileId AS ownerProfileId, " +
                "TouristId AS touristId, PaymentPlanId AS paymentPlanId, Status AS status " +
                "FROM payment_transactions WHERE Id=?", id);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch " + id);
        }
        Map<String, Object> payment = rows.get(0);
        String status = String.valueOf(payment.get("status"));
        if ("paid".equalsIgnoreCase(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Giao dịch này đã được xác nhận trước đó.");
        }

        jdbc.update("UPDATE payment_transactions SET Status='Paid', PaidAt=CURRENT_TIMESTAMP WHERE Id=?", id);
        // Menu-order demo payments are linked by the transaction Note; do not activate a subscription for them.
        List<Map<String, Object>> paymentPurposeRows = jdbc.queryForList("SELECT Purpose AS purpose, Note AS note FROM payment_transactions WHERE Id=?", id);
        if (!paymentPurposeRows.isEmpty() && "MenuOrder".equalsIgnoreCase(String.valueOf(paymentPurposeRows.get(0).get("purpose")))) {
            String note = String.valueOf(paymentPurposeRows.get(0).get("note"));
            if (note.matches("MenuOrderId=[0-9]+")) {
                long menuOrderId = Long.parseLong(note.substring("MenuOrderId=".length()));
                jdbc.update("UPDATE menu_orders SET PaymentStatus='Paid' WHERE Id=? AND lower(PaymentStatus)!='paid'", menuOrderId);
            }
            log("UPDATE", "payment_transactions", id, "Xác nhận thanh toán demo cho đơn menu");
            return Map.of("success", true, "id", id, "status", "Paid", "message", "Đã xác nhận thanh toán demo cho đơn menu.");
        }
        Object planValue = payment.get("paymentPlanId");
        if (planValue != null) {
            long planId = ((Number) planValue).longValue();
            List<Map<String, Object>> plans = jdbc.queryForList(
                    "SELECT Id AS id, DurationDays AS durationDays FROM payment_plans WHERE Id=?", planId);
            if (!plans.isEmpty()) {
                int durationDays = integer(plans.get(0).get("durationDays"), 30);
                if (durationDays <= 0) durationDays = 30;
                String payerType = String.valueOf(payment.get("payerType"));
                Object ownerValue = payment.get("ownerProfileId");
                Object touristValue = payment.get("touristId");
                if ("Owner".equalsIgnoreCase(payerType) && ownerValue instanceof Number ownerNumber) {
                    long ownerId = ownerNumber.longValue();
                    jdbc.update("UPDATE owner_subscriptions SET Status='Expired' WHERE OwnerProfileId=? AND lower(Status)='active'", ownerId);
                    jdbc.update("INSERT INTO owner_subscriptions(OwnerProfileId, PaymentPlanId, PaymentTransactionId, Status, StartsAt, ExpiresAt) " +
                            "VALUES(?,?,?,'Active',CURRENT_TIMESTAMP,datetime(CURRENT_TIMESTAMP, ?))",
                            ownerId, planId, id, "+" + durationDays + " days");
                } else if ("Tourist".equalsIgnoreCase(payerType) && touristValue instanceof Number touristNumber) {
                    long touristId = touristNumber.longValue();
                    jdbc.update("UPDATE tourist_subscriptions SET Status='Expired' WHERE TouristId=? AND lower(Status)='active'", touristId);
                    jdbc.update("INSERT INTO tourist_subscriptions(TouristId, PaymentPlanId, PaymentTransactionId, Status, StartsAt, ExpiresAt) " +
                            "VALUES(?,?,?,'Active',CURRENT_TIMESTAMP,datetime(CURRENT_TIMESTAMP, ?))",
                            touristId, planId, id, "+" + durationDays + " days");
                }
            }
        }
        log("UPDATE", "payment_transactions", id, "Xác nhận thanh toán và kích hoạt gói nếu có");
        return Map.of("success", true, "id", id, "status", "Paid", "message", "Đã xác nhận giao dịch.");
    }

    /** Mirrors the original C# PaymentController.Reject action. */
    @ResponseBody
    @PostMapping("/api/payments/{id}/reject")
    public Map<String, Object> rejectPayment(@PathVariable long id) {
        int updated = jdbc.update("UPDATE payment_transactions SET Status='Rejected' WHERE Id=? AND lower(Status)!='paid'", id);
        if (updated == 0) {
            Long exists = jdbc.query("SELECT Id FROM payment_transactions WHERE Id=?", rs -> rs.next() ? rs.getLong(1) : null, id);
            if (exists != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Giao dịch đã thanh toán, không thể từ chối.");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch " + id);
        }
        List<Map<String, Object>> rejectedRows = jdbc.queryForList("SELECT Purpose AS purpose, Note AS note FROM payment_transactions WHERE Id=?", id);
        if (!rejectedRows.isEmpty() && "MenuOrder".equalsIgnoreCase(String.valueOf(rejectedRows.get(0).get("purpose")))) {
            String note = String.valueOf(rejectedRows.get(0).get("note"));
            if (note.matches("MenuOrderId=[0-9]+")) {
                long menuOrderId = Long.parseLong(note.substring("MenuOrderId=".length()));
                jdbc.update("UPDATE menu_orders SET PaymentStatus='Unpaid', PaymentMethod='PayAtCounter' WHERE Id=? AND lower(PaymentStatus)!='paid'", menuOrderId);
            }
        }
        log("UPDATE", "payment_transactions", id, "Từ chối giao dịch thanh toán");
        return Map.of("success", true, "id", id, "status", "Rejected", "message", "Đã từ chối giao dịch.");
    }

    @ResponseBody @GetMapping("/api/owners")
    public List<Map<String, Object>> owners() {
        return jdbc.queryForList("SELECT Id AS id, UserId AS userId, BusinessName AS businessName, RepresentativeName AS representativeName, Phone AS phone, Address AS address, Status AS status, CreatedAt AS createdAt FROM owner_profiles ORDER BY Id DESC");
    }

    @ResponseBody @GetMapping("/api/sync-versions")
    public List<Map<String, Object>> syncVersions() {
        return jdbc.queryForList("SELECT Id AS id, VersionNumber AS versionNumber, Description AS description, CreatedAt AS createdAt FROM sync_versions ORDER BY Id DESC");
    }

    @ResponseBody @PostMapping("/api/sync-versions")
    public Map<String, Object> createSyncVersion(@RequestBody Map<String, Object> body) {
        String version = required(body, "versionNumber");
        String description = String.valueOf(body.getOrDefault("description", ""));
        jdbc.update("INSERT INTO sync_versions(VersionNumber, Description, CreatedAt) VALUES(?,?,CURRENT_TIMESTAMP)", version, description);
        Long id = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
        log("INSERT", "sync_versions", id, "Tạo phiên bản đồng bộ " + version);
        return Map.of("success", true, "id", id == null ? 0 : id, "versionNumber", version);
    }

    @ResponseBody @GetMapping("/api/lan-traffic")
    public Map<String, Object> lanTraffic() {
        return lanVisitorTrackingService.dashboardStats();
    }

    @ResponseBody @GetMapping("/api/lan-visitors")
    public List<Map<String, Object>> lanVisitors() {
        return lanVisitorTrackingService.onlineLanVisitors();
    }

    @ResponseBody @GetMapping("/api/visitor-traffic")
    public List<Map<String, Object>> visitorTraffic(@RequestParam(defaultValue = "30") int days) {
        int safeDays = Math.max(1, Math.min(days, 365));
        return jdbc.queryForList("SELECT date(CreatedAt) AS date, COUNT(*) AS plays, COUNT(DISTINCT DeviceId) AS devices, COUNT(DISTINCT PoiId) AS poiCount FROM visitor_playback_logs WHERE CreatedAt >= datetime('now', ?) GROUP BY date(CreatedAt) ORDER BY date", "-" + safeDays + " days");
    }

    // Full CRUD endpoints added in the next migration pass. They keep the existing SQLite schema.
    @ResponseBody @PostMapping("/api/pois")
    public Map<String, Object> createPoi(@RequestBody Map<String, Object> body) {
        double latitude = decimal(body.get("latitude"), "latitude");
        double longitude = decimal(body.get("longitude"), "longitude");
        double radius = body.containsKey("radius") ? decimal(body.get("radius"), "radius") : 30.0;
        String status = String.valueOf(body.getOrDefault("status", "Pending"));
        String adminNote = String.valueOf(body.getOrDefault("adminNote", ""));
        String coverImageUrl = String.valueOf(body.getOrDefault("coverImageUrl", ""));
        jdbc.update("INSERT INTO pois(Latitude, Longitude, Radius, QrCodeToken, Status, AdminNote, CoverImageUrl, CreatedAt) VALUES(?,?,?,?,?,?,?,CURRENT_TIMESTAMP)", latitude, longitude, Math.round(radius), UUID.randomUUID().toString(), status, adminNote, coverImageUrl);
        Long id = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
        if (id == null) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không lấy được ID POI vừa tạo");
        if (body.containsKey("name") && !String.valueOf(body.get("name")).isBlank()) {
            Map<String, String> translation = new java.util.HashMap<>();
            translation.put("languageCode", String.valueOf(body.getOrDefault("languageCode", "vi")));
            translation.put("name", String.valueOf(body.get("name")));
            translation.put("shortDescription", String.valueOf(body.getOrDefault("shortDescription", "")));
            translation.put("fullDescription", String.valueOf(body.getOrDefault("fullDescription", "")));
            translation.put("ttsScript", String.valueOf(body.getOrDefault("ttsScript", body.getOrDefault("fullDescription", ""))));
            translation.put("audioUrl", String.valueOf(body.getOrDefault("audioUrl", "")));
            translation.put("audioDuration", String.valueOf(body.getOrDefault("audioDuration", 0)));
            translation.put("videoUrl", String.valueOf(body.getOrDefault("videoUrl", "")));
            savePoiTranslation(id, translation);
        }
        log("INSERT", "pois", id, "Tạo POI mới");
        return Map.of("success", true, "id", id, "status", status);
    }

    @ResponseBody @PutMapping("/api/pois/{id}")
    public Map<String, Object> updatePoi(@PathVariable long id, @RequestBody Map<String, Object> body) {
        requireExists("pois", id);
        Map<String, Object> current = jdbc.queryForMap("SELECT Latitude AS latitude, Longitude AS longitude, Radius AS radius, Status AS status, AdminNote AS adminNote, CoverImageUrl AS coverImageUrl FROM pois WHERE Id=?", id);
        double latitude = body.containsKey("latitude") ? decimal(body.get("latitude"), "latitude") : decimal(current.get("latitude"), "latitude");
        double longitude = body.containsKey("longitude") ? decimal(body.get("longitude"), "longitude") : decimal(current.get("longitude"), "longitude");
        double radius = body.containsKey("radius") ? decimal(body.get("radius"), "radius") : decimal(current.get("radius"), "radius");
        String status = String.valueOf(body.getOrDefault("status", current.get("status")));
        String note = String.valueOf(body.getOrDefault("adminNote", current.get("adminNote") == null ? "" : current.get("adminNote")));
        String cover = String.valueOf(body.getOrDefault("coverImageUrl", current.get("coverImageUrl") == null ? "" : current.get("coverImageUrl")));
        jdbc.update("UPDATE pois SET Latitude=?, Longitude=?, Radius=?, Status=?, AdminNote=?, CoverImageUrl=? WHERE Id=?", latitude, longitude, radius, status, note, cover, id);
        log("UPDATE", "pois", id, "Cập nhật thông tin POI");
        return Map.of("success", true, "id", id, "status", status);
    }

    @ResponseBody @GetMapping("/api/pois/{id}/translations")
    public List<Map<String, Object>> getPoiTranslations(@PathVariable long id) {
        requireExists("pois", id);
        return jdbc.queryForList("SELECT Id AS id, PoiId AS poiId, LanguageCode AS languageCode, Name AS name, ShortDescription AS shortDescription, FullDescription AS fullDescription, TtsScript AS ttsScript, AudioUrl AS audioUrl, AudioDuration AS audioDuration, VideoUrl AS videoUrl, UpdatedAt AS updatedAt FROM poi_translations WHERE PoiId=? ORDER BY LanguageCode", id);
    }

    @ResponseBody @DeleteMapping("/api/pois/{poiId}/translations/{translationId}")
    public Map<String, Object> deletePoiTranslation(@PathVariable long poiId, @PathVariable long translationId) {
        int changed = jdbc.update("DELETE FROM poi_translations WHERE Id=? AND PoiId=?", translationId, poiId);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bản dịch POI");
        log("DELETE", "poi_translations", translationId, "Xóa bản dịch POI " + poiId);
        return Map.of("success", true);
    }

    @ResponseBody @PostMapping("/api/tours")
    public Map<String, Object> createTour(@RequestBody Map<String, Object> body) {
        String status = String.valueOf(body.getOrDefault("status", "Active"));
        int estimatedTime = integer(body.get("estimatedTime"), 0);
        jdbc.update("INSERT INTO tours(EstimatedTime, Status, CreatedAt) VALUES(?,?,CURRENT_TIMESTAMP)", estimatedTime, status);
        Long id = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
        if (id == null) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không lấy được ID tour vừa tạo");
        if (body.containsKey("title") && !String.valueOf(body.get("title")).isBlank()) {
            upsertTourTranslation(id, Map.of("languageCode", String.valueOf(body.getOrDefault("languageCode", "vi")), "title", String.valueOf(body.get("title")), "description", String.valueOf(body.getOrDefault("description", ""))));
        }
        log("INSERT", "tours", id, "Tạo tour mới");
        return Map.of("success", true, "id", id, "status", status);
    }

    @ResponseBody @PutMapping("/api/tours/{id}")
    public Map<String, Object> updateTour(@PathVariable long id, @RequestBody Map<String, Object> body) {
        requireExists("tours", id);
        Map<String, Object> current = jdbc.queryForMap("SELECT EstimatedTime AS estimatedTime, Status AS status FROM tours WHERE Id=?", id);
        int estimatedTime = body.containsKey("estimatedTime") ? integer(body.get("estimatedTime"), 0) : integer(current.get("estimatedTime"), 0);
        String status = String.valueOf(body.getOrDefault("status", current.get("status")));
        jdbc.update("UPDATE tours SET EstimatedTime=?, Status=? WHERE Id=?", estimatedTime, status, id);
        log("UPDATE", "tours", id, "Cập nhật tour");
        return Map.of("success", true, "id", id, "status", status);
    }

    @ResponseBody @DeleteMapping("/api/tours/{id}")
    public Map<String, Object> deleteTour(@PathVariable long id) {
        requireExists("tours", id);
        jdbc.update("DELETE FROM tour_pois WHERE TourId=?", id);
        jdbc.update("DELETE FROM tour_translations WHERE TourId=?", id);
        jdbc.update("DELETE FROM tours WHERE Id=?", id);
        log("DELETE", "tours", id, "Xóa tour và các bản dịch/POI liên kết");
        return Map.of("success", true);
    }

    @ResponseBody @GetMapping("/api/tours/{id}/translations")
    public List<Map<String, Object>> getTourTranslations(@PathVariable long id) {
        requireExists("tours", id);
        return jdbc.queryForList("SELECT Id AS id, TourId AS tourId, LanguageCode AS languageCode, Title AS title, Description AS description FROM tour_translations WHERE TourId=? ORDER BY LanguageCode", id);
    }

    @ResponseBody @PostMapping("/api/tours/{id}/translations")
    public Map<String, Object> saveTourTranslation(@PathVariable long id, @RequestBody Map<String, Object> body) {
        requireExists("tours", id);
        String language = required(body, "languageCode").toLowerCase();
        String title = required(body, "title");
        String description = String.valueOf(body.getOrDefault("description", ""));
        Integer existing = jdbc.query("SELECT Id FROM tour_translations WHERE TourId=? AND lower(LanguageCode)=? LIMIT 1", rs -> rs.next() ? rs.getInt(1) : null, id, language);
        if (existing == null) jdbc.update("INSERT INTO tour_translations(TourId, LanguageCode, Title, Description) VALUES(?,?,?,?)", id, language, title, description);
        else jdbc.update("UPDATE tour_translations SET Title=?, Description=? WHERE Id=?", title, description, existing);
        log("UPSERT", "tour_translations", id, "Lưu bản dịch tour " + language);
        return Map.of("success", true, "tourId", id, "languageCode", language);
    }

    @ResponseBody @DeleteMapping("/api/tours/{tourId}/translations/{translationId}")
    public Map<String, Object> deleteTourTranslation(@PathVariable long tourId, @PathVariable long translationId) {
        int changed = jdbc.update("DELETE FROM tour_translations WHERE Id=? AND TourId=?", translationId, tourId);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bản dịch tour");
        log("DELETE", "tour_translations", translationId, "Xóa bản dịch tour " + tourId);
        return Map.of("success", true);
    }

    @ResponseBody @GetMapping("/api/categories")
    public List<Map<String, Object>> categories(@RequestParam(defaultValue = "vi") String lang) {
        return jdbc.queryForList("SELECT c.Id AS id, c.IconUrl AS iconUrl, c.Status AS status, COALESCE(ct.Name, cv.Name, '') AS name, COALESCE(ct.LanguageCode, cv.LanguageCode, 'vi') AS languageCode FROM categories c LEFT JOIN category_translations ct ON ct.CategoryId=c.Id AND lower(ct.LanguageCode)=? LEFT JOIN category_translations cv ON cv.CategoryId=c.Id AND lower(cv.LanguageCode)='vi' ORDER BY c.Id DESC", lang.toLowerCase());
    }

    @ResponseBody @PostMapping("/api/categories")
    public Map<String, Object> createCategory(@RequestBody Map<String, Object> body) {
        String status = String.valueOf(body.getOrDefault("status", "Active"));
        String iconUrl = String.valueOf(body.getOrDefault("iconUrl", ""));
        jdbc.update("INSERT INTO categories(IconUrl, Status) VALUES(?,?)", iconUrl, status);
        Long id = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
        if (id == null) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không lấy được ID category");
        if (body.containsKey("name") && !String.valueOf(body.get("name")).isBlank()) {
            upsertCategoryTranslation(id, Map.of("languageCode", String.valueOf(body.getOrDefault("languageCode", "vi")), "name", String.valueOf(body.get("name"))));
        }
        log("INSERT", "categories", id, "Tạo danh mục");
        return Map.of("success", true, "id", id);
    }

    @ResponseBody @PutMapping("/api/categories/{id}")
    public Map<String, Object> updateCategory(@PathVariable long id, @RequestBody Map<String, Object> body) {
        requireExists("categories", id);
        Map<String, Object> current = jdbc.queryForMap("SELECT IconUrl AS iconUrl, Status AS status FROM categories WHERE Id=?", id);
        jdbc.update("UPDATE categories SET IconUrl=?, Status=? WHERE Id=?", String.valueOf(body.getOrDefault("iconUrl", current.get("iconUrl"))), String.valueOf(body.getOrDefault("status", current.get("status"))), id);
        if (body.containsKey("name")) upsertCategoryTranslation(id, Map.of("languageCode", String.valueOf(body.getOrDefault("languageCode", "vi")), "name", String.valueOf(body.get("name"))));
        log("UPDATE", "categories", id, "Cập nhật danh mục");
        return Map.of("success", true, "id", id);
    }

    @ResponseBody @DeleteMapping("/api/categories/{id}")
    public Map<String, Object> deleteCategory(@PathVariable long id) {
        requireExists("categories", id);
        jdbc.update("DELETE FROM poi_categories WHERE CategoryId=?", id);
        jdbc.update("DELETE FROM category_translations WHERE CategoryId=?", id);
        jdbc.update("DELETE FROM categories WHERE Id=?", id);
        log("DELETE", "categories", id, "Xóa danh mục và liên kết");
        return Map.of("success", true);
    }

    @ResponseBody @PostMapping("/api/categories/{id}/translations")
    public Map<String, Object> saveCategoryTranslation(@PathVariable long id, @RequestBody Map<String, Object> body) {
        requireExists("categories", id);
        String language = required(body, "languageCode").toLowerCase();
        String name = required(body, "name");
        Integer existing = jdbc.query("SELECT Id FROM category_translations WHERE CategoryId=? AND lower(LanguageCode)=? LIMIT 1", rs -> rs.next() ? rs.getInt(1) : null, id, language);
        if (existing == null) jdbc.update("INSERT INTO category_translations(CategoryId, LanguageCode, Name) VALUES(?,?,?)", id, language, name);
        else jdbc.update("UPDATE category_translations SET Name=? WHERE Id=?", name, existing);
        log("UPSERT", "category_translations", id, "Lưu bản dịch danh mục " + language);
        return Map.of("success", true, "categoryId", id, "languageCode", language);
    }

    @ResponseBody @PostMapping("/api/pois/{id}/categories/{categoryId}")
    public Map<String, Object> assignPoiCategory(@PathVariable long id, @PathVariable long categoryId) {
        requireExists("pois", id); requireExists("categories", categoryId);
        jdbc.update("INSERT INTO poi_categories(PoiId, CategoryId) VALUES(?,?) ON CONFLICT(PoiId, CategoryId) DO NOTHING", id, categoryId);
        return Map.of("success", true, "poiId", id, "categoryId", categoryId);
    }

    @ResponseBody @DeleteMapping("/api/pois/{id}/categories/{categoryId}")
    public Map<String, Object> removePoiCategory(@PathVariable long id, @PathVariable long categoryId) {
        jdbc.update("DELETE FROM poi_categories WHERE PoiId=? AND CategoryId=?", id, categoryId);
        return Map.of("success", true);
    }

    @ResponseBody @GetMapping("/api/pois/{id}/categories")
    public List<Map<String, Object>> poiCategories(@PathVariable long id, @RequestParam(defaultValue = "vi") String lang) {
        requireExists("pois", id);
        return jdbc.queryForList("SELECT c.Id AS id, c.IconUrl AS iconUrl, c.Status AS status, COALESCE(t.Name, tv.Name, '') AS name FROM poi_categories pc JOIN categories c ON c.Id=pc.CategoryId LEFT JOIN category_translations t ON t.CategoryId=c.Id AND lower(t.LanguageCode)=? LEFT JOIN category_translations tv ON tv.CategoryId=c.Id AND lower(tv.LanguageCode)='vi' WHERE pc.PoiId=? ORDER BY name", lang.toLowerCase(), id);
    }

    private void upsertTourTranslation(long tourId, Map<String, Object> body) {
        String language = required(body, "languageCode").toLowerCase();
        String title = required(body, "title");
        String description = String.valueOf(body.getOrDefault("description", ""));
        Integer existing = jdbc.query("SELECT Id FROM tour_translations WHERE TourId=? AND lower(LanguageCode)=? LIMIT 1", rs -> rs.next() ? rs.getInt(1) : null, tourId, language);
        if (existing == null) jdbc.update("INSERT INTO tour_translations(TourId, LanguageCode, Title, Description) VALUES(?,?,?,?)", tourId, language, title, description);
        else jdbc.update("UPDATE tour_translations SET Title=?, Description=? WHERE Id=?", title, description, existing);
    }

    private void upsertCategoryTranslation(long categoryId, Map<String, Object> body) {
        String language = required(body, "languageCode").toLowerCase();
        String name = required(body, "name");
        Integer existing = jdbc.query("SELECT Id FROM category_translations WHERE CategoryId=? AND lower(LanguageCode)=? LIMIT 1", rs -> rs.next() ? rs.getInt(1) : null, categoryId, language);
        if (existing == null) jdbc.update("INSERT INTO category_translations(CategoryId, LanguageCode, Name) VALUES(?,?,?)", categoryId, language, name);
        else jdbc.update("UPDATE category_translations SET Name=? WHERE Id=?", name, existing);
    }



    // Pass 4: owner/POI claim administration, menu orders, and tourist discovery reporting.
    @ResponseBody
    @GetMapping("/api/owner-requests")
    public List<Map<String, Object>> ownerRequests(@RequestParam(defaultValue = "") String status) {
        return jdbc.queryForList("""
            SELECT r.Id AS id, r.OwnerProfileId AS ownerProfileId, o.BusinessName AS businessName,
                   r.PoiId AS poiId, COALESCE(pt.Name, '') AS poiName, r.RequestType AS requestType,
                   r.Status AS status, r.Note AS note, r.CreatedAt AS createdAt, r.ReviewedAt AS reviewedAt
            FROM poi_owner_requests r
            JOIN owner_profiles o ON o.Id=r.OwnerProfileId
            LEFT JOIN poi_translations pt ON pt.PoiId=r.PoiId AND lower(pt.LanguageCode)='vi'
            WHERE (?='' OR lower(r.Status)=lower(?))
            ORDER BY r.CreatedAt DESC, r.Id DESC
            """, status, status);
    }

    @ResponseBody
    @PostMapping("/api/owner-requests/{id}/approve")
    public Map<String, Object> approveOwnerRequest(@PathVariable long id) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT Id AS id, OwnerProfileId AS ownerProfileId, PoiId AS poiId, RequestType AS requestType, Status AS status FROM poi_owner_requests WHERE Id=?", id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu");
        Map<String, Object> req = rows.get(0);
        Object poiValue = req.get("poiId");
        if (poiValue == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yêu cầu chưa gắn với POI");
        long poiId = ((Number) poiValue).longValue();
        long ownerId = ((Number) req.get("ownerProfileId")).longValue();
        jdbc.update("UPDATE pois SET OwnerProfileId=? WHERE Id=?", ownerId, poiId);
        String requestType = String.valueOf(req.get("requestType"));
        if ("Create".equalsIgnoreCase(requestType)) {
            jdbc.update("UPDATE pois SET AdminNote=CASE WHEN lower(Status)='pending' THEN 'Đã xác nhận quyền chủ gian hàng; POI vẫn cần duyệt nội dung.' ELSE AdminNote END WHERE Id=?", poiId);
        }
        jdbc.update("UPDATE poi_owner_requests SET Status='Approved', ReviewedAt=CURRENT_TIMESTAMP WHERE Id=?", id);
        log("UPDATE", "poi_owner_requests", id, "Duyệt yêu cầu chủ gian hàng/POI");
        return Map.of("success", true, "id", id, "status", "Approved", "poiId", poiId, "ownerProfileId", ownerId);
    }

    @ResponseBody
    @PostMapping("/api/owner-requests/{id}/reject")
    public Map<String, Object> rejectOwnerRequest(@PathVariable long id, @RequestBody(required = false) Map<String, Object> body) {
        requireExists("poi_owner_requests", id);
        String note = body == null || body.get("note") == null ? "" : body.get("note").toString().trim();
        if (note.isBlank()) {
            jdbc.update("UPDATE poi_owner_requests SET Status='Rejected', ReviewedAt=CURRENT_TIMESTAMP WHERE Id=?", id);
        } else {
            jdbc.update("UPDATE poi_owner_requests SET Status='Rejected', ReviewedAt=CURRENT_TIMESTAMP, Note=CASE WHEN COALESCE(Note,'')='' THEN ? ELSE Note || char(10) || 'Admin: ' || ? END WHERE Id=?", note, note, id);
        }
        log("UPDATE", "poi_owner_requests", id, "Từ chối yêu cầu chủ gian hàng/POI");
        return Map.of("success", true, "id", id, "status", "Rejected");
    }

    @ResponseBody
    @GetMapping("/api/menu-orders")
    public List<Map<String, Object>> menuOrders(@RequestParam(defaultValue = "") String status,
                                                 @RequestParam(required = false) Long poiId,
                                                 @RequestParam(defaultValue = "200") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 1000));
        return jdbc.queryForList("""
            SELECT m.Id AS id, m.OrderCode AS orderCode, m.TouristId AS touristId,
                   m.OwnerProfileId AS ownerProfileId, o.BusinessName AS businessName,
                   m.PoiId AS poiId, COALESCE(pt.Name,'') AS poiName,
                   m.CustomerName AS customerName, m.CustomerPhone AS customerPhone,
                   m.Status AS status, m.PaymentStatus AS paymentStatus, m.PaymentMethod AS paymentMethod,
                   m.Subtotal AS subtotal, m.TotalAmount AS totalAmount, m.Currency AS currency,
                   m.CreatedAt AS createdAt, m.ConfirmedAt AS confirmedAt, m.CompletedAt AS completedAt,
                   m.CancelledAt AS cancelledAt
            FROM menu_orders m
            LEFT JOIN owner_profiles o ON o.Id=m.OwnerProfileId
            LEFT JOIN poi_translations pt ON pt.PoiId=m.PoiId AND lower(pt.LanguageCode)='vi'
            WHERE (?='' OR lower(m.Status)=lower(?)) AND (? IS NULL OR m.PoiId=?)
            ORDER BY m.CreatedAt DESC, m.Id DESC LIMIT ?
            """, status, status, poiId, poiId, safeLimit);
    }

    @ResponseBody
    @GetMapping("/api/menu-orders/{id}")
    public Map<String, Object> menuOrderDetail(@PathVariable long id) {
        List<Map<String, Object>> orders = jdbc.queryForList("SELECT Id AS id, OrderCode AS orderCode, TouristId AS touristId, OwnerProfileId AS ownerProfileId, PoiId AS poiId, CustomerName AS customerName, CustomerPhone AS customerPhone, Note AS note, Status AS status, PaymentMethod AS paymentMethod, PaymentStatus AS paymentStatus, Subtotal AS subtotal, TotalAmount AS totalAmount, Currency AS currency, CreatedAt AS createdAt, ConfirmedAt AS confirmedAt, CompletedAt AS completedAt, CancelledAt AS cancelledAt FROM menu_orders WHERE Id=?", id);
        if (orders.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng");
        Map<String, Object> order = orders.get(0);
        order.put("items", jdbc.queryForList("SELECT Id AS id, OwnerMenuItemId AS menuItemId, ItemName AS itemName, UnitPrice AS unitPrice, Quantity AS quantity, LineTotal AS lineTotal, Currency AS currency FROM menu_order_items WHERE MenuOrderId=? ORDER BY Id", id));
        return order;
    }

    /** Admin/demo workflow for menu orders. State transitions are deliberately explicit. */
    @ResponseBody
    @Transactional
    @PostMapping("/api/menu-orders/{id}/confirm")
    public Map<String, Object> confirmMenuOrder(@PathVariable long id) {
        Map<String, Object> order = menuOrderState(id);
        String status = String.valueOf(order.get("status"));
        if ("Cancelled".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể xác nhận đơn ở trạng thái " + status);
        }
        if ("Paid".equalsIgnoreCase(String.valueOf(order.get("paymentStatus"))) || "PayAtCounter".equalsIgnoreCase(String.valueOf(order.get("paymentMethod")))) {
            int changed = jdbc.update("UPDATE menu_orders SET Status='Confirmed', ConfirmedAt=COALESCE(ConfirmedAt,CURRENT_TIMESTAMP) WHERE Id=? AND lower(Status)='pending'", id);
            if (changed == 0 && !"Confirmed".equalsIgnoreCase(status)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Trạng thái đơn đã thay đổi, vui lòng tải lại");
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn thanh toán online cần thanh toán thành công trước khi xác nhận");
        }
        log("UPDATE", "menu_orders", id, "Xác nhận đơn hàng menu");
        return Map.of("success", true, "id", id, "status", "Confirmed");
    }

    @ResponseBody
    @Transactional
    @PostMapping("/api/menu-orders/{id}/complete")
    public Map<String, Object> completeMenuOrder(@PathVariable long id) {
        Map<String, Object> order = menuOrderState(id);
        String status = String.valueOf(order.get("status"));
        if ("Completed".equalsIgnoreCase(status)) return Map.of("success", true, "id", id, "status", "Completed");
        if (!"Confirmed".equalsIgnoreCase(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ có thể hoàn thành đơn đã xác nhận");
        }
        jdbc.update("UPDATE menu_orders SET Status='Completed', CompletedAt=CURRENT_TIMESTAMP WHERE Id=? AND lower(Status)='confirmed'", id);
        log("UPDATE", "menu_orders", id, "Hoàn thành đơn hàng menu");
        return Map.of("success", true, "id", id, "status", "Completed");
    }

    @ResponseBody
    @Transactional
    @PostMapping("/api/menu-orders/{id}/cancel")
    public Map<String, Object> adminCancelMenuOrder(@PathVariable long id, @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> order = menuOrderState(id);
        String status = String.valueOf(order.get("status"));
        String paymentStatus = String.valueOf(order.get("paymentStatus"));
        if ("Completed".equalsIgnoreCase(status)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể hủy đơn đã hoàn thành");
        if ("Cancelled".equalsIgnoreCase(status)) return Map.of("success", true, "id", id, "status", "Cancelled");
        if ("Paid".equalsIgnoreCase(paymentStatus)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn đã thanh toán; cần xử lý hoàn tiền riêng trước khi hủy");
        String note = body == null || body.get("note") == null ? "" : body.get("note").toString().trim();
        jdbc.update("UPDATE menu_orders SET Status='Cancelled', CancelledAt=CURRENT_TIMESTAMP, Note=CASE WHEN ?='' THEN Note WHEN COALESCE(Note,'')='' THEN ? ELSE Note || char(10) || 'Admin: ' || ? END WHERE Id=? AND lower(Status) NOT IN ('cancelled','completed')", note, note, note, id);
        log("UPDATE", "menu_orders", id, "Hủy đơn hàng menu");
        return Map.of("success", true, "id", id, "status", "Cancelled");
    }

    private Map<String, Object> menuOrderState(long id) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT Id AS id, Status AS status, PaymentStatus AS paymentStatus, PaymentMethod AS paymentMethod FROM menu_orders WHERE Id=?", id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng " + id);
        return rows.get(0);
    }

    @ResponseBody
    @GetMapping("/api/tourist-discoveries")
    public List<Map<String, Object>> touristDiscoveries(@RequestParam(required = false) Long touristId,
                                                         @RequestParam(required = false) Long poiId,
                                                         @RequestParam(defaultValue = "200") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 1000));
        return jdbc.queryForList("""
            SELECT d.TouristId AS touristId, d.PoiId AS poiId, COALESCE(pt.Name,'') AS poiName,
                   d.DiscoveryMethod AS discoveryMethod, d.PointsAwarded AS pointsAwarded,
                   d.VisitorLatitude AS visitorLatitude, d.VisitorLongitude AS visitorLongitude,
                   d.DiscoveredAt AS discoveredAt
            FROM tourist_poi_discoveries d
            LEFT JOIN poi_translations pt ON pt.PoiId=d.PoiId AND lower(pt.LanguageCode)='vi'
            WHERE (? IS NULL OR d.TouristId=?) AND (? IS NULL OR d.PoiId=?)
            ORDER BY d.DiscoveredAt DESC LIMIT ?
            """, touristId, touristId, poiId, poiId, safeLimit);
    }

    @ResponseBody
    @GetMapping("/api/owner-menu-items")
    public List<Map<String, Object>> ownerMenuItems(@RequestParam(required = false) Long poiId,
                                                     @RequestParam(defaultValue = "") String status) {
        return jdbc.queryForList("""
            SELECT m.Id AS id, m.OwnerProfileId AS ownerProfileId, o.BusinessName AS businessName,
                   m.PoiId AS poiId, COALESCE(pt.Name,'') AS poiName, m.Name AS name,
                   m.Description AS description, m.Price AS price, m.Currency AS currency,
                   m.ImageUrl AS imageUrl, m.Status AS status, m.CreatedAt AS createdAt, m.UpdatedAt AS updatedAt
            FROM owner_menu_items m
            LEFT JOIN owner_profiles o ON o.Id=m.OwnerProfileId
            LEFT JOIN poi_translations pt ON pt.PoiId=m.PoiId AND lower(pt.LanguageCode)='vi'
            WHERE (? IS NULL OR m.PoiId=?) AND (?='' OR lower(m.Status)=lower(?))
            ORDER BY m.PoiId, m.Name
            """, poiId, poiId, status, status);
    }

    private void requireExists(String table, long id) {
        // table is supplied only by controller methods, never directly from user input.
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE Id=?", Long.class, id);
        if (count == null || count == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bản ghi " + id + " trong " + table);
    }

    private long countSql(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
    }

    private double decimal(Object value, String field) {
        try { return Double.parseDouble(String.valueOf(value)); }
        catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " không hợp lệ"); }
    }


    private void log(String action, String table, Object targetId, String description) {
        try { jdbc.update("INSERT INTO admin_activity_logs(UserId, Action, TargetTable, TargetId, Description, CreatedAt) VALUES(NULL,?,?,?,?,CURRENT_TIMESTAMP)", action, table, targetId, description); }
        catch (Exception ignored) { /* logging must not break the requested action */ }
    }
    private String required(Map<String, ?> body, String key) {
        Object value = body.get(key);
        if (value == null || value.toString().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu trường: " + key);
        return value.toString().trim();
    }
    private int integer(Object value, int fallback) {
        try { return value == null ? fallback : Integer.parseInt(value.toString()); }
        catch (NumberFormatException ex) { return fallback; }
    }
    private long longValue(Object value) {
        try { return Long.parseLong(String.valueOf(value)); }
        catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "poiId không hợp lệ"); }
    }

    // Owner/vendor management API migrated from OwnerAdminController.cs.
    @ResponseBody @GetMapping("/api/owners/{id}")
    public Map<String, Object> ownerDetail(@PathVariable int id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT Id AS id, UserId AS userId, BusinessName AS businessName, RepresentativeName AS representativeName, Phone AS phone, Address AS address, Status AS status, CreatedAt AS createdAt FROM owner_profiles WHERE Id=?", id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy chủ gian hàng");
        return rows.get(0);
    }

    @ResponseBody @PostMapping("/api/owners")
    public Map<String, Object> createOwner(@RequestBody Map<String, Object> body) {
        int userId = integer(body.get("userId"), -1);
        if (userId < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId không hợp lệ");
        requireExists("users", userId);
        String businessName = required(body, "businessName");
        String status = String.valueOf(body.getOrDefault("status", "Pending"));
        jdbc.update("INSERT INTO owner_profiles(UserId, BusinessName, RepresentativeName, Phone, Address, Status, CreatedAt) VALUES(?,?,?,?,?,?,CURRENT_TIMESTAMP)",
            userId, businessName, body.get("representativeName"), body.get("phone"), body.get("address"), status);
        Long id = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
        log("CREATE", "owner_profiles", id, "Tạo hồ sơ chủ gian hàng");
        return Map.of("success", true, "id", id == null ? 0 : id, "status", status);
    }

    @ResponseBody @PutMapping("/api/owners/{id}")
    public Map<String, Object> updateOwner(@PathVariable int id, @RequestBody Map<String, Object> body) {
        requireExists("owner_profiles", id);
        String businessName = required(body, "businessName");
        String status = String.valueOf(body.getOrDefault("status", "Pending"));
        jdbc.update("UPDATE owner_profiles SET BusinessName=?, RepresentativeName=?, Phone=?, Address=?, Status=? WHERE Id=?",
            businessName, body.get("representativeName"), body.get("phone"), body.get("address"), status, id);
        log("UPDATE", "owner_profiles", id, "Cập nhật hồ sơ chủ gian hàng");
        return Map.of("success", true, "id", id, "status", status);
    }

    @ResponseBody @PostMapping("/api/owners/{id}/approve")
    public Map<String, Object> approveOwner(@PathVariable int id) {
        requireExists("owner_profiles", id);
        jdbc.update("UPDATE owner_profiles SET Status='Approved' WHERE Id=?", id);
        log("APPROVE", "owner_profiles", id, "Duyệt chủ gian hàng");
        return Map.of("success", true, "id", id, "status", "Approved");
    }

    @ResponseBody @PostMapping("/api/owners/{id}/lock")
    public Map<String, Object> lockOwner(@PathVariable int id) {
        requireExists("owner_profiles", id);
        jdbc.update("UPDATE owner_profiles SET Status='Locked' WHERE Id=?", id);
        log("LOCK", "owner_profiles", id, "Khóa chủ gian hàng");
        return Map.of("success", true, "id", id, "status", "Locked");
    }

    @ResponseBody @PostMapping("/api/owners/{id}/pois/{poiId}")
    public Map<String, Object> assignPoiToOwner(@PathVariable int id, @PathVariable int poiId) {
        requireExists("owner_profiles", id);
        requireExists("pois", poiId);
        jdbc.update("UPDATE pois SET OwnerProfileId=? WHERE Id=?", id, poiId);
        log("ASSIGN_POI", "pois", poiId, "Gán POI cho owner " + id);
        return Map.of("success", true, "ownerId", id, "poiId", poiId);
    }

    @ResponseBody @DeleteMapping("/api/owners/{id}/pois/{poiId}")
    public Map<String, Object> removePoiFromOwner(@PathVariable int id, @PathVariable int poiId) {
        requireExists("owner_profiles", id);
        requireExists("pois", poiId);
        jdbc.update("UPDATE pois SET OwnerProfileId=NULL WHERE Id=? AND OwnerProfileId=?", poiId, id);
        log("UNASSIGN_POI", "pois", poiId, "Gỡ POI khỏi owner " + id);
        return Map.of("success", true, "ownerId", id, "poiId", poiId);
    }

    @ResponseBody @GetMapping("/api/owners/{id}/pois")
    public List<Map<String, Object>> ownerPois(@PathVariable int id) {
        requireExists("owner_profiles", id);
        return jdbc.queryForList("SELECT p.Id AS id, p.Status AS status, p.Latitude AS latitude, p.Longitude AS longitude, t.Name AS name FROM pois p LEFT JOIN poi_translations t ON t.PoiId=p.Id AND t.LanguageCode='vi' WHERE p.OwnerProfileId=? ORDER BY p.Id", id);
    }

    @ResponseBody @PutMapping("/api/pois/{poiId}/media/order")
    public Map<String, Object> reorderPoiMedia(@PathVariable int poiId, @RequestBody Map<String, Object> body) {
        requireExists("pois", poiId);
        Object raw = body.get("mediaIds");
        if (!(raw instanceof List<?> ids)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mediaIds phải là một danh sách");
        int order = 0;
        for (Object value : ids) {
            int mediaId = integer(value, -1);
            if (mediaId < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mediaId không hợp lệ");
            int changed = jdbc.update("UPDATE media_assets SET SortOrder=? WHERE Id=? AND PoiId=?", order++, mediaId, poiId);
            if (changed == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Media không thuộc POI đã chọn: " + mediaId);
        }
        log("REORDER", "media_assets", poiId, "Sắp xếp media của POI");
        return Map.of("success", true, "poiId", poiId, "updated", order);
    }


    // Revenue and purchase reporting migrated into the consolidated admin controller.
    @ResponseBody
    @GetMapping("/api/revenue/summary")
    public Map<String, Object> revenueSummary() {
        Map<String, Object> summary = jdbc.queryForMap(
                "SELECT COUNT(*) AS transactionTotal, " +
                "COALESCE(SUM(CASE WHEN lower(Status) IN ('paid','completed','success','succeeded') " +
                "THEN CAST(Amount AS REAL) ELSE 0 END), 0) AS paidAmount, " +
                "COALESCE(SUM(CASE WHEN lower(Status) IN ('pending','processing') THEN 1 ELSE 0 END), 0) AS pendingTransactions, " +
                "COALESCE(SUM(CASE WHEN lower(Status) IN ('failed','cancelled','canceled','expired') THEN 1 ELSE 0 END), 0) AS failedTransactions " +
                "FROM payment_transactions");
        return Map.of("success", true, "data", summary);
    }

    @ResponseBody
    @GetMapping("/api/revenue/transactions")
    public Map<String, Object> revenueTransactions(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "") String status) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        int safeOffset = Math.max(0, offset);
        List<Map<String, Object>> rows;
        if (status == null || status.isBlank()) {
            rows = jdbc.queryForList(
                    "SELECT pt.Id AS id, pt.TransactionCode AS transactionCode, pt.PayerType AS payerType, " +
                    "pt.OwnerProfileId AS ownerProfileId, pt.TouristId AS touristId, pt.PaymentPlanId AS paymentPlanId, " +
                    "pp.PlanName AS planName, pt.Purpose AS purpose, pt.Amount AS amount, pt.Currency AS currency, " +
                    "pt.PaymentMethod AS paymentMethod, pt.Status AS status, pt.CreatedAt AS createdAt, pt.PaidAt AS paidAt " +
                    "FROM payment_transactions pt LEFT JOIN payment_plans pp ON pp.Id=pt.PaymentPlanId " +
                    "ORDER BY pt.Id DESC LIMIT ? OFFSET ?", safeLimit, safeOffset);
        } else {
            rows = jdbc.queryForList(
                    "SELECT pt.Id AS id, pt.TransactionCode AS transactionCode, pt.PayerType AS payerType, " +
                    "pt.OwnerProfileId AS ownerProfileId, pt.TouristId AS touristId, pt.PaymentPlanId AS paymentPlanId, " +
                    "pp.PlanName AS planName, pt.Purpose AS purpose, pt.Amount AS amount, pt.Currency AS currency, " +
                    "pt.PaymentMethod AS paymentMethod, pt.Status AS status, pt.CreatedAt AS createdAt, pt.PaidAt AS paidAt " +
                    "FROM payment_transactions pt LEFT JOIN payment_plans pp ON pp.Id=pt.PaymentPlanId " +
                    "WHERE lower(pt.Status)=lower(?) ORDER BY pt.Id DESC LIMIT ? OFFSET ?", status.trim(), safeLimit, safeOffset);
        }
        return Map.of("success", true, "count", rows.size(), "data", rows, "limit", safeLimit, "offset", safeOffset);
    }

    @ResponseBody
    @GetMapping("/api/purchases")
    public Map<String, Object> purchases(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        int safeOffset = Math.max(0, offset);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT pt.Id AS id, pt.TransactionCode AS transactionCode, pt.PayerType AS payerType, " +
                "pt.TouristId AS touristId, pt.OwnerProfileId AS ownerProfileId, pt.PaymentPlanId AS paymentPlanId, " +
                "pp.PlanName AS planName, pt.Purpose AS purpose, pt.Amount AS amount, pt.Currency AS currency, " +
                "pt.PaymentMethod AS paymentMethod, pt.Status AS status, pt.CreatedAt AS createdAt, pt.PaidAt AS paidAt " +
                "FROM payment_transactions pt LEFT JOIN payment_plans pp ON pp.Id=pt.PaymentPlanId " +
                "WHERE lower(pt.Purpose) LIKE '%subscription%' OR lower(pt.Purpose) LIKE '%purchase%' " +
                "ORDER BY pt.Id DESC LIMIT ? OFFSET ?", safeLimit, safeOffset);
        return Map.of("success", true, "count", rows.size(), "data", rows, "limit", safeLimit, "offset", safeOffset);
    }

@GetMapping(value = "/api/pois/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
@ResponseBody
public ResponseEntity<byte[]> getPoiQr(@PathVariable long id) {
    try {
        // Kiem tra POI co ton tai khong
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT Id, Status FROM pois WHERE Id = ?", id
        );

        if (rows.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Lay IP LAN cua may tinh dang chay website
        String ip = InetAddress.getLocalHost().getHostAddress();

        // URL trang chi tiet POI
        String url = "http://" + ip + ":8080/pois/" + id;

        // Tao anh QR
        QRCodeWriter qrWriter = new QRCodeWriter();
        var matrix = qrWriter.encode(
            url,
            BarcodeFormat.QR_CODE,
            300,
            300
        );

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(
            matrix,
            "PNG",
            output
        );

        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .body(output.toByteArray());

    } catch (WriterException | java.io.IOException ex) {
        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Không thể tạo mã QR",
            ex
        );
    }
}


}
