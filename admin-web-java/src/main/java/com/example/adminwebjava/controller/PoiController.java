package com.example.adminwebjava.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

/** Public website and consolidated public/mobile API routes migrated from the C# controllers. */
@Controller
public class PoiController {
    private final TourGuideService service;
    private final JdbcTemplate jdbc;

    public PoiController(TourGuideService service, JdbcTemplate jdbc) {
        this.service = service;
        this.jdbc = jdbc;
    }

    @GetMapping("/pois/qr/{token}")
    public String openPoiByQr(@PathVariable String token) {
        Long poiId = jdbc.query(
            "SELECT Id FROM pois WHERE QrCodeToken = ? " +
         "AND lower(Status) = 'approved' LIMIT 1",
            rs -> rs.next() ? rs.getLong("Id") : null,
            token
        );

        if (poiId == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Mã QR không hợp lệ hoặc địa điểm chưa được duyệt"
            );
    }

    return "redirect:/pois/" + poiId;
}


    @GetMapping({"/", "/home"})
    public String home(@RequestParam(defaultValue = "vi") String lang, Model model) {
        model.addAttribute("lang", lang);
        model.addAttribute("pois", service.findPois(lang, "", "Approved"));
        model.addAttribute("categories", service.findCategories(lang));
        model.addAttribute("tours", service.findTours(lang));
        return "poi/home";
    }

    @GetMapping("/pois")
    public String listPois(@RequestParam(defaultValue = "vi") String lang,
                           @RequestParam(defaultValue = "") String keyword,
                           @RequestParam(defaultValue = "") String status,
                           Model model) {
        model.addAttribute("lang", lang);
        model.addAttribute("keyword", keyword);
        model.addAttribute("pois", service.findPois(lang, keyword, status.isBlank() ? "Approved" : status));
        return "poi/list";
    }

    @GetMapping("/pois/{id}")
    public String poiDetail(@PathVariable long id, @RequestParam(defaultValue = "vi") String lang, Model model) {
        Map<String, Object> poi = service.findPoi(id, lang);
        if (poi == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy địa điểm");
        model.addAttribute("lang", lang);
        model.addAttribute("poi", poi);
        model.addAttribute("media", jdbc.queryForList("SELECT Id AS id, MediaType AS mediaType, MediaUrl AS mediaUrl, SortOrder AS sortOrder FROM media_assets WHERE PoiId=? ORDER BY SortOrder, Id", id));
        model.addAttribute("reviews", jdbc.queryForList("SELECT Id AS id, TouristId AS touristId, Rating AS rating, Comment AS comment, CreatedAt AS createdAt FROM poi_reviews WHERE PoiId=? ORDER BY Id DESC LIMIT 100", id));
        return "pois/detail";
    }

    @GetMapping("/tours")
    public String tours(@RequestParam(defaultValue = "vi") String lang, Model model) {
        model.addAttribute("lang", lang);
        model.addAttribute("tours", service.findTours(lang));
        return "pois/tours";
    }

    // Mobile/public APIs migrated from TourApiController and supporting API controllers.
    /** Public menu endpoint migrated from MenuOrderApiController.GetPoiMenu in the C# project. */
    @ResponseBody
    @GetMapping("/api/menu/poi/{poiId}")
    public Map<String, Object> getPoiMenu(@PathVariable long poiId) {
        Long approved = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pois WHERE Id=? AND Status='Approved'", Long.class, poiId);
        if (approved == null || approved == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "POI không tồn tại hoặc chưa được duyệt.");
        }
        List<Map<String, Object>> items = jdbc.queryForList("""
            SELECT Id AS id, PoiId AS poiId, Name AS name,
                   COALESCE(Description, '') AS description, Price AS price,
                   Currency AS currency, ImageUrl AS imageUrl
            FROM owner_menu_items
            WHERE PoiId=? AND lower(Status)='active'
            ORDER BY CAST(Price AS NUMERIC), Name
            """, poiId);
        return Map.of("success", true, "data", items);
    }

    /** Create a menu order. This local demo API accepts touristId explicitly; the C# app obtains it from JWT claims. */
    @ResponseBody
    @PostMapping("/api/menu/orders")
    @Transactional
    public Map<String, Object> createMenuOrder(@RequestBody Map<String, Object> body) {
        long touristId = requiredLong(body.get("touristId"), "touristId");
        long poiId = requiredLong(body.get("poiId"), "poiId");
        String phone = stringValue(body.get("customerPhone")).trim();
        if (phone.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập số điện thoại để chủ gian hàng xác nhận đơn.");
        Long touristCount = jdbc.queryForObject("SELECT COUNT(*) FROM tourists WHERE Id=?", Long.class, touristId);
        if (touristCount == null || touristCount == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy du khách.");
        Map<String, Object> poi = jdbc.query("SELECT Id AS id, OwnerProfileId AS ownerProfileId FROM pois WHERE Id=? AND lower(Status)='approved'", rs -> rs.next() ? Map.<String, Object>of("id", rs.getLong("id"), "ownerProfileId", rs.getObject("ownerProfileId") == null ? 0L : rs.getLong("ownerProfileId")) : null, poiId);
        if (poi == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "POI không tồn tại hoặc chưa được duyệt.");
        long ownerId = ((Number) poi.get("ownerProfileId")).longValue();
        if (ownerId <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "POI này chưa có chủ gian hàng nên chưa thể đặt món.");

        Object rawItems = body.get("items");
        if (!(rawItems instanceof List<?> requestItems)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "items phải là danh sách món.");
        Map<Long, Integer> selected = new LinkedHashMap<>();
        for (Object item : requestItems) {
            if (!(item instanceof Map<?, ?> itemMap)) continue;
            long menuItemId = requiredLong(itemMap.get("menuItemId"), "menuItemId");
            int quantity;
            try { quantity = Integer.parseInt(String.valueOf(itemMap.get("quantity"))); }
            catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity không hợp lệ."); }
            if (quantity > 0) selected.merge(menuItemId, quantity, (a, b) -> Math.min(a + b, 20));
        }
        if (selected.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn ít nhất 1 món/sản phẩm.");

        List<Map<String, Object>> orderLines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        String currency = "VND";
        for (Map.Entry<Long, Integer> entry : selected.entrySet()) {
            List<Map<String, Object>> found = jdbc.queryForList("SELECT Id AS id, Name AS name, Price AS price, Currency AS currency FROM owner_menu_items WHERE Id=? AND PoiId=? AND lower(Status)='active'", entry.getKey(), poiId);
            if (found.isEmpty()) continue;
            Map<String, Object> menuItem = found.get(0);
            BigDecimal price = new BigDecimal(String.valueOf(menuItem.get("price")));
            int quantity = entry.getValue();
            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(quantity));
            currency = String.valueOf(menuItem.getOrDefault("currency", "VND"));
            orderLines.add(Map.of("menuItemId", entry.getKey(), "itemName", String.valueOf(menuItem.get("name")), "unitPrice", price, "quantity", quantity, "lineTotal", lineTotal, "currency", currency));
            subtotal = subtotal.add(lineTotal);
        }
        if (orderLines.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Các món đã chọn không còn khả dụng.");

        String touristName = jdbc.query("SELECT FullName FROM tourists WHERE Id=?", rs -> rs.next() ? rs.getString(1) : null, touristId);
        String customerName = stringValue(body.get("customerName")).trim();
        if (customerName.isBlank()) customerName = touristName == null || touristName.isBlank() ? "Du khách" : touristName.trim();
        if (customerName.length() > 160) customerName = customerName.substring(0, 160);
        if (phone.length() > 40) phone = phone.substring(0, 40);
        String note = stringValue(body.get("note")).trim();
        if (note.isBlank()) note = null;
        String orderCode = ("MNU-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16)).toUpperCase();
        jdbc.update("INSERT INTO menu_orders(OrderCode,TouristId,OwnerProfileId,PoiId,CustomerName,CustomerPhone,Note,Status,PaymentMethod,PaymentStatus,Subtotal,TotalAmount,Currency,CreatedAt) VALUES(?,?,?,?,?,?,?,'Pending','PayAtCounter','Unpaid',?,?,?,CURRENT_TIMESTAMP)", orderCode, touristId, ownerId, poiId, customerName, phone, note, subtotal.toPlainString(), subtotal.toPlainString(), currency);
        Long orderId = jdbc.queryForObject("SELECT Id FROM menu_orders WHERE OrderCode=?", Long.class, orderCode);
        for (Map<String, Object> line : orderLines) {
            jdbc.update("INSERT INTO menu_order_items(MenuOrderId,OwnerMenuItemId,ItemName,UnitPrice,Quantity,LineTotal,Currency) VALUES(?,?,?,?,?,?,?)", orderId, line.get("menuItemId"), line.get("itemName"), String.valueOf(line.get("unitPrice")), line.get("quantity"), String.valueOf(line.get("lineTotal")), line.get("currency"));
        }
        return Map.of("success", true, "message", "Đã tạo đơn. Chủ gian hàng sẽ xác nhận đơn.", "data", loadMenuOrder(orderId, touristId));
    }

    /** Starts a local demo payment for a menu order; no real payment provider is contacted. */
    @ResponseBody
    @PostMapping("/api/menu/orders/{id}/checkout")
    @Transactional
    public Map<String, Object> checkoutMenuOrder(@PathVariable long id, @RequestParam long touristId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT Id AS id, OrderCode AS orderCode, Status AS status, PaymentStatus AS paymentStatus, TotalAmount AS totalAmount, Currency AS currency FROM menu_orders WHERE Id=? AND TouristId=?", id, touristId);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn.");
        Map<String, Object> order = rows.get(0);
        String status = String.valueOf(order.get("status"));
        String paymentStatus = String.valueOf(order.get("paymentStatus"));
        if (status.equalsIgnoreCase("Cancelled") || status.equalsIgnoreCase("Completed"))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn đã đóng, không thể thanh toán.");
        if (paymentStatus.equalsIgnoreCase("Paid"))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn đã thanh toán.");
        Long existing = jdbc.query("SELECT Id FROM payment_transactions WHERE Purpose='MenuOrder' AND Note=? AND lower(Status)='pending' ORDER BY Id DESC LIMIT 1", rs -> rs.next() ? rs.getLong(1) : null, "MenuOrderId=" + id);
        if (existing != null) return Map.of("success", true, "message", "Đã có giao dịch demo đang chờ xác nhận.", "paymentId", existing, "paymentStatus", "Pending", "demoOnly", true);
        String code = ("MNU-PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 14)).toUpperCase();
        jdbc.update("INSERT INTO payment_transactions(TransactionCode,PayerType,TouristId,Purpose,Amount,Currency,PaymentMethod,Status,Note,CreatedAt) VALUES(?,'Tourist',?,'MenuOrder',?,?,?,'Pending',?,CURRENT_TIMESTAMP)",
                code, touristId, String.valueOf(order.get("totalAmount")), String.valueOf(order.get("currency")), "Demo", "MenuOrderId=" + id);
        Long paymentId = jdbc.queryForObject("SELECT Id FROM payment_transactions WHERE TransactionCode=?", Long.class, code);
        jdbc.update("UPDATE menu_orders SET PaymentMethod='DemoOnline', PaymentStatus='Pending' WHERE Id=? AND TouristId=?", id, touristId);
        return Map.of("success", true, "message", "Đã tạo giao dịch thanh toán DEMO. Chưa có tiền thật được thu.", "paymentId", paymentId, "transactionCode", code, "paymentStatus", "Pending", "demoOnly", true);
    }

    @ResponseBody
    @GetMapping("/api/menu/orders/my")
    public Map<String, Object> getMyMenuOrders(@RequestParam long touristId) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM tourists WHERE Id=?", Long.class, touristId);
        if (count == null || count == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy du khách.");
        List<Long> ids = jdbc.queryForList("SELECT Id FROM menu_orders WHERE TouristId=? ORDER BY CreatedAt DESC, Id DESC", Long.class, touristId);
        List<Map<String, Object>> orders = ids.stream().map(id -> loadMenuOrder(id, touristId)).toList();
        return Map.of("success", true, "data", orders);
    }

    @ResponseBody
    @GetMapping("/api/menu/orders/{id}")
    public Map<String, Object> getMenuOrder(@PathVariable long id, @RequestParam long touristId) {
        Map<String, Object> order = loadMenuOrder(id, touristId);
        if (order == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn.");
        return Map.of("success", true, "data", order);
    }

    @ResponseBody
    @PostMapping("/api/menu/orders/{id}/cancel")
    @Transactional
    public Map<String, Object> cancelMenuOrder(@PathVariable long id, @RequestParam long touristId) {
        Map<String, Object> order = jdbc.query("SELECT Status AS status, PaymentStatus AS paymentStatus FROM menu_orders WHERE Id=? AND TouristId=?", rs -> rs.next() ? Map.<String, Object>of("status", rs.getString("status"), "paymentStatus", rs.getString("paymentStatus")) : null, id, touristId);
        if (order == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn.");
        String status = String.valueOf(order.get("status"));
        String paymentStatus = String.valueOf(order.get("paymentStatus"));
        if (status.equalsIgnoreCase("Completed") || status.equalsIgnoreCase("Cancelled")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn này không thể hủy.");
        if (paymentStatus.equalsIgnoreCase("Paid")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn đã thanh toán online. Vui lòng liên hệ chủ gian hàng để xử lý hoàn tiền/hủy đơn.");
        jdbc.update("UPDATE menu_orders SET Status='Cancelled', CancelledAt=CURRENT_TIMESTAMP WHERE Id=? AND TouristId=?", id, touristId);
        return Map.of("success", true, "data", true, "message", "Đã hủy đơn.");
    }

    private Map<String, Object> loadMenuOrder(long orderId, long touristId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT mo.Id AS id, mo.OrderCode AS orderCode, mo.TouristId AS touristId, mo.PoiId AS poiId, COALESCE(pt.Name,'POI #' || mo.PoiId) AS poiName, op.BusinessName AS ownerName, mo.CustomerName AS customerName, mo.CustomerPhone AS customerPhone, mo.Status AS status, mo.PaymentMethod AS paymentMethod, mo.PaymentStatus AS paymentStatus, mo.TotalAmount AS totalAmount, mo.Currency AS currency, mo.Note AS note, mo.CreatedAt AS createdAt FROM menu_orders mo LEFT JOIN pois p ON p.Id=mo.PoiId LEFT JOIN poi_translations pt ON pt.PoiId=p.Id AND lower(pt.LanguageCode)='vi' LEFT JOIN owner_profiles op ON op.Id=mo.OwnerProfileId WHERE mo.Id=? AND mo.TouristId=? LIMIT 1", orderId, touristId);
        if (rows.isEmpty()) return null;
        Map<String, Object> order = new java.util.LinkedHashMap<>(rows.get(0));
        order.put("items", jdbc.queryForList("SELECT Id AS id, OwnerMenuItemId AS menuItemId, ItemName AS itemName, UnitPrice AS unitPrice, Quantity AS quantity, LineTotal AS lineTotal, Currency AS currency FROM menu_order_items WHERE MenuOrderId=? ORDER BY Id", orderId));
        return order;
    }

    private static long requiredLong(Object value, String field) {
        try {
            long parsed = Long.parseLong(String.valueOf(value));
            if (parsed <= 0) throw new NumberFormatException();
            return parsed;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " không hợp lệ.");
        }
    }

    private static String stringValue(Object value) { return value == null ? "" : String.valueOf(value); }

    @ResponseBody @GetMapping("/api/tour")
    public Map<String, Object> apiTours(@RequestParam(defaultValue = "vi") String lang) {
        return Map.of("success", true, "data", service.findTours(lang));
    }

    @ResponseBody @GetMapping("/api/tour/{tourId}/pois")
    public Map<String, Object> apiTourPois(@PathVariable long tourId, @RequestParam(defaultValue = "vi") String lang) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT tp.TourId AS tourId, tp.PoiId AS poiId, tp.SequenceOrder AS sequenceOrder, p.Latitude AS latitude, p.Longitude AS longitude, p.Radius AS radius, p.CoverImageUrl AS coverImageUrl, COALESCE(pt.Name, vt.Name, '(Chưa có bản dịch)') AS name, COALESCE(pt.ShortDescription, vt.ShortDescription, '') AS shortDescription FROM tour_pois tp JOIN pois p ON p.Id=tp.PoiId LEFT JOIN poi_translations pt ON pt.PoiId=p.Id AND lower(pt.LanguageCode)=? LEFT JOIN poi_translations vt ON vt.PoiId=p.Id AND lower(vt.LanguageCode)='vi' WHERE tp.TourId=? ORDER BY tp.SequenceOrder", lang.toLowerCase(), tourId);
        return Map.of("success", true, "data", rows);
    }

    @ResponseBody @GetMapping("/api/poi/{poiId}/reviews")
    public Map<String, Object> getReviews(@PathVariable long poiId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT Id AS id, TouristId AS touristId, Rating AS rating, Comment AS comment, CreatedAt AS createdAt FROM poi_reviews WHERE PoiId=? ORDER BY Id DESC", poiId);
        double average = rows.stream().mapToDouble(r -> ((Number) r.get("rating")).doubleValue()).average().orElse(0.0);
        return Map.of("success", true, "data", Map.of("poiId", poiId, "count", rows.size(), "averageRating", average, "reviews", rows));
    }

    @ResponseBody @PostMapping("/api/poi/{poiId}/reviews")
    public Map<String, Object> submitReview(@PathVariable long poiId, @RequestBody Map<String, Object> body) {
        int rating;
        try { rating = Integer.parseInt(String.valueOf(body.get("rating"))); }
        catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rating phải là số từ 1 đến 5"); }
        if (rating < 1 || rating > 5) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rating phải từ 1 đến 5");
        long touristId;
        try { touristId = Long.parseLong(String.valueOf(body.get("touristId"))); }
        catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "touristId không hợp lệ"); }
        String comment = String.valueOf(body.getOrDefault("comment", ""));
        if (comment.length() > 600) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bình luận tối đa 600 ký tự");
        Long approved = jdbc.queryForObject("SELECT COUNT(*) FROM pois WHERE Id=? AND lower(Status)='approved'", Long.class, poiId);
        if (approved == null || approved == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "POI không tồn tại hoặc chưa được duyệt");
        Integer existingReviewId = jdbc.query("SELECT Id FROM poi_reviews WHERE PoiId=? AND TouristId=? LIMIT 1", rs -> rs.next() ? rs.getInt(1) : null, poiId, touristId);
        if (existingReviewId == null) {
            jdbc.update("INSERT INTO poi_reviews(PoiId, TouristId, Rating, Comment, CreatedAt) VALUES(?,?,?,?,CURRENT_TIMESTAMP)", poiId, touristId, rating, comment);
        } else {
            jdbc.update("UPDATE poi_reviews SET Rating=?, Comment=?, CreatedAt=CURRENT_TIMESTAMP WHERE Id=?", rating, comment, existingReviewId);
        }
        List<Map<String, Object>> reviews = jdbc.queryForList("SELECT Id AS id, TouristId AS touristId, Rating AS rating, Comment AS comment, CreatedAt AS createdAt FROM poi_reviews WHERE PoiId=? ORDER BY Id DESC", poiId);
        double average = reviews.stream().mapToDouble(r -> ((Number) r.get("rating")).doubleValue()).average().orElse(0.0);
        return Map.of("success", true, "message", "Cảm ơn bạn đã gửi đánh giá.", "data", Map.of("poiId", poiId, "count", reviews.size(), "averageRating", Math.round(average * 10.0) / 10.0, "reviews", reviews));
    }

    @ResponseBody @PostMapping("/api/log")
    public Map<String, Object> logPlayback(@RequestBody Map<String, Object> body) {
        long poiId = number(body.get("poiId"), "poiId");
        Object tourist = body.get("touristId");
        Object duration = body.getOrDefault("listenDuration", 0);
        Object device = body.getOrDefault("deviceId", "");
        Object lang = body.getOrDefault("languageCode", "vi");
        Object trigger = body.getOrDefault("triggerType", "unknown");
        jdbc.update("INSERT INTO visitor_playback_logs(PoiId, TouristId, DeviceId, LanguageCode, TriggerType, ListenDuration, CreatedAt) VALUES(?,?,?,?,?,?,CURRENT_TIMESTAMP)",
                poiId, tourist == null ? null : number(tourist, "touristId"), String.valueOf(device), String.valueOf(lang), String.valueOf(trigger), (int) number(duration, "listenDuration"));
        return Map.of("success", true);
    }

    @ResponseBody @GetMapping("/api/recent")
    public Map<String, Object> recent(@RequestParam(defaultValue = "") String deviceId,
                                      @RequestParam(required = false) Long touristId) {
        List<Map<String, Object>> rows;
        if (touristId != null) {
            rows = jdbc.queryForList("SELECT PoiId AS poiId, MAX(CreatedAt) AS lastPlayedAt FROM visitor_playback_logs WHERE TouristId=? GROUP BY PoiId ORDER BY lastPlayedAt DESC LIMIT 100", touristId);
        } else if (!deviceId.isBlank()) {
            rows = jdbc.queryForList("SELECT PoiId AS poiId, MAX(CreatedAt) AS lastPlayedAt FROM visitor_playback_logs WHERE DeviceId=? GROUP BY PoiId ORDER BY lastPlayedAt DESC LIMIT 100", deviceId);
        } else {
            rows = List.of();
        }
        return Map.of("success", true, "data", rows);
    }

    @ResponseBody @PostMapping("/api/recent")
    public Map<String, Object> addRecent(@RequestBody Map<String, Object> body) {
        long poiId = number(body.get("poiId"), "poiId");
        String deviceId = String.valueOf(body.getOrDefault("deviceId", ""));
        Long touristId = body.get("touristId") == null ? null : number(body.get("touristId"), "touristId");
        jdbc.update("INSERT INTO visitor_playback_logs(PoiId, TouristId, DeviceId, LanguageCode, TriggerType, ListenDuration, CreatedAt) VALUES(?,?,?,?,?,0,CURRENT_TIMESTAMP)", poiId, touristId, deviceId, String.valueOf(body.getOrDefault("languageCode", "vi")), "recent");
        return Map.of("success", true);
    }

    @ResponseBody @GetMapping("/api/user/me")
    public Map<String, Object> currentUser(@RequestParam(required = false) Long touristId) {
        if (touristId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cần touristId trong bản demo Java");
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT Id AS id, Email AS email, FullName AS fullName, AuthProvider AS authProvider, CreatedAt AS createdAt FROM tourists WHERE Id=?", touristId);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản du khách");
        return Map.of("success", true, "data", rows.get(0));
    }

    @ResponseBody @GetMapping("/api/tourist/plans")
    public Map<String, Object> touristPlans() {
        return Map.of("success", true, "data", jdbc.queryForList("SELECT Id AS id, PlanCode AS planCode, PlanName AS planName, Price AS price, DurationDays AS durationDays, Description AS description FROM payment_plans WHERE IsActive=1 AND lower(Audience) IN ('tourist','visitor','all') ORDER BY Price"));
    }

    @ResponseBody @GetMapping("/api/ui-translations")
    public Map<String, Object> uiTranslations(@RequestParam(defaultValue = "vi") String languageCode,
                                               @RequestParam(defaultValue = "web") String platform) {
        // The supplied SQLite schema stores POI/category/tour translations, not the C# UI translation cache.
        return Map.of("success", true, "languageCode", languageCode, "platform", platform, "data", Map.of());
    }

    @ResponseBody @GetMapping("/api/ui-translations/keys")
    public Map<String, Object> uiTranslationKeys() {
        return Map.of("success", true, "data", List.of());
    }

    // Pass 5: tourist experience APIs migrated from TouristExperienceApiController.
    @ResponseBody @GetMapping("/api/tourist/premium")
    public Map<String, Object> touristPremium(@RequestParam Long touristId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT s.Id AS subscriptionId, s.Status AS status, s.StartsAt AS startsAt, s.ExpiresAt AS expiresAt, p.PlanCode AS planCode, p.PlanName AS planName " +
                "FROM tourist_subscriptions s LEFT JOIN payment_plans p ON p.Id=s.PaymentPlanId " +
                "WHERE s.TouristId=? AND lower(s.Status)='active' ORDER BY s.Id DESC", touristId);
        boolean premium = rows.stream().anyMatch(r -> {
            Object expires = r.get("expiresAt");
            return expires == null || String.valueOf(expires).isBlank() || String.valueOf(expires).compareTo(java.time.LocalDateTime.now().toString()) >= 0;
        });
        return Map.of("success", true, "data", Map.of("touristId", touristId, "isPremium", premium, "subscriptions", rows));
    }

    @ResponseBody @GetMapping("/api/tourist/discoveries")
    public Map<String, Object> touristDiscoveries(@RequestParam Long touristId,
                                                   @RequestParam(defaultValue = "vi") String languageCode) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT d.PoiId AS poiId, d.DiscoveryMethod AS discoveryMethod, d.PointsAwarded AS pointsAwarded, d.DiscoveredAt AS discoveredAt, " +
                "COALESCE(pt.Name, vt.Name, '(Chưa có bản dịch)') AS name " +
                "FROM tourist_poi_discoveries d LEFT JOIN poi_translations pt ON pt.PoiId=d.PoiId AND lower(pt.LanguageCode)=? " +
                "LEFT JOIN poi_translations vt ON vt.PoiId=d.PoiId AND lower(vt.LanguageCode)='vi' " +
                "WHERE d.TouristId=? ORDER BY d.DiscoveredAt DESC", languageCode.toLowerCase(), touristId);
        int points = rows.stream().mapToInt(r -> r.get("pointsAwarded") instanceof Number n ? n.intValue() : 0).sum();
        return Map.of("success", true, "data", Map.of("touristId", touristId, "totalDiscoveries", rows.size(), "totalPoints", points, "items", rows));
    }

    @ResponseBody @GetMapping("/api/tourist/bookmarks")
    public Map<String, Object> touristBookmarks(@RequestParam Long touristId,
                                                 @RequestParam(defaultValue = "vi") String languageCode) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT b.Id AS bookmarkId, b.PoiId AS poiId, b.CreatedAt AS createdAt, COALESCE(pt.Name, vt.Name, '(Chưa có bản dịch)') AS name " +
                "FROM tourist_bookmarks b LEFT JOIN poi_translations pt ON pt.PoiId=b.PoiId AND lower(pt.LanguageCode)=? " +
                "LEFT JOIN poi_translations vt ON vt.PoiId=b.PoiId AND lower(vt.LanguageCode)='vi' " +
                "WHERE b.TouristId=? ORDER BY b.CreatedAt DESC", languageCode.toLowerCase(), touristId);
        return Map.of("success", true, "data", rows);
    }

    @ResponseBody @PostMapping("/api/tourist/bookmarks")
    public Map<String, Object> addTouristBookmark(@RequestBody Map<String, Object> body) {
        long touristId = number(body.get("touristId"), "touristId");
        long poiId = number(body.get("poiId"), "poiId");
        Long exists = jdbc.queryForObject("SELECT COUNT(*) FROM tourists WHERE Id=?", Long.class, touristId);
        if (exists == null || exists == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy du khách");
        Long poiExists = jdbc.queryForObject("SELECT COUNT(*) FROM pois WHERE Id=?", Long.class, poiId);
        if (poiExists == null || poiExists == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy POI");
        Long bookmarkExists = jdbc.queryForObject("SELECT COUNT(*) FROM tourist_bookmarks WHERE TouristId=? AND PoiId=?", Long.class, touristId, poiId);
        if (bookmarkExists == null || bookmarkExists == 0) {
            jdbc.update("INSERT INTO tourist_bookmarks(TouristId, PoiId, CreatedAt) VALUES(?,?,CURRENT_TIMESTAMP)", touristId, poiId);
        }
        return Map.of("success", true, "touristId", touristId, "poiId", poiId);
    }

    @ResponseBody @DeleteMapping("/api/tourist/bookmarks/{poiId}")
    public Map<String, Object> removeTouristBookmark(@PathVariable long poiId, @RequestParam Long touristId) {
        int deleted = jdbc.update("DELETE FROM tourist_bookmarks WHERE TouristId=? AND PoiId=?", touristId, poiId);
        return Map.of("success", true, "deleted", deleted > 0, "touristId", touristId, "poiId", poiId);
    }

    @ResponseBody @GetMapping("/api/tourist/favorites")
    public Map<String, Object> touristFavorites(@RequestParam Long touristId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT TargetType AS targetType, TargetId AS targetId, CreatedAt AS createdAt FROM tourist_favorites WHERE TouristId=? ORDER BY CreatedAt DESC", touristId);
        return Map.of("success", true, "data", rows);
    }

    @ResponseBody @PostMapping("/api/tourist/favorites")
    public Map<String, Object> addTouristFavorite(@RequestBody Map<String, Object> body) {
        long touristId = number(body.get("touristId"), "touristId");
        long targetId = number(body.get("targetId"), "targetId");
        String targetType = String.valueOf(body.getOrDefault("targetType", "Poi")).trim();
        if (targetType.isBlank()) targetType = "Poi";
        if (!targetType.equalsIgnoreCase("Poi") && !targetType.equalsIgnoreCase("Tour"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetType phải là Poi hoặc Tour");
        Long userExists = jdbc.queryForObject("SELECT COUNT(*) FROM tourists WHERE Id=?", Long.class, touristId);
        if (userExists == null || userExists == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy du khách");
        String table = targetType.equalsIgnoreCase("Poi") ? "pois" : "tours";
        Long targetExists = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE Id=?", Long.class, targetId);
        if (targetExists == null || targetExists == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy mục yêu thích");
        jdbc.update("INSERT OR IGNORE INTO tourist_favorites(TouristId, TargetType, TargetId, CreatedAt) VALUES(?,?,?,CURRENT_TIMESTAMP)", touristId, targetType, targetId);
        return Map.of("success", true, "touristId", touristId, "targetType", targetType, "targetId", targetId);
    }

    @ResponseBody @DeleteMapping("/api/tourist/favorites/{targetType}/{targetId}")
    public Map<String, Object> removeTouristFavorite(@PathVariable String targetType, @PathVariable long targetId,
                                                       @RequestParam Long touristId) {
        int deleted = jdbc.update("DELETE FROM tourist_favorites WHERE TouristId=? AND lower(TargetType)=lower(?) AND TargetId=?", touristId, targetType, targetId);
        return Map.of("success", true, "deleted", deleted > 0);
    }

    private long number(Object value, String field) {
        try { return Long.parseLong(String.valueOf(value)); }
        catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " không hợp lệ"); }
    }
}
