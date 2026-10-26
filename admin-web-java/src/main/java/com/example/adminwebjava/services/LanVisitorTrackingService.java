package com.example.adminwebjava.services;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LanVisitorTrackingService implements HandlerInterceptor {
    private static final int DISPLAY_OFFSET = 0;
    private static final int ONLINE_MINUTES = 5;
    private final JdbcTemplate jdbc;

    public LanVisitorTrackingService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void init() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS web_visit_sessions (" +
                "SessionId TEXT PRIMARY KEY, " +
                "IpAddress TEXT NOT NULL, " +
                "UserAgent TEXT, " +
                "FirstSeen TEXT NOT NULL, " +
                "LastSeen TEXT NOT NULL, " +
                "LastPath TEXT, " +
                "Role TEXT)");
        jdbc.update("CREATE INDEX IF NOT EXISTS idx_web_visit_last_seen ON web_visit_sessions(LastSeen)");
        jdbc.update("CREATE INDEX IF NOT EXISTS idx_web_visit_first_seen ON web_visit_sessions(FirstSeen)");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) {
        touch(request);
        return true;
    }

    public void touch(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (shouldIgnore(path)) return;

        HttpSession session = request.getSession(true);
        String sessionId = session.getId();
        String ip = resolveClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String role = session.getAttribute("authRole") == null ? "Guest" : String.valueOf(session.getAttribute("authRole"));

        jdbc.update("INSERT INTO web_visit_sessions(SessionId, IpAddress, UserAgent, FirstSeen, LastSeen, LastPath, Role) " +
                        "VALUES(?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,?,?) " +
                        "ON CONFLICT(SessionId) DO UPDATE SET IpAddress=excluded.IpAddress, UserAgent=excluded.UserAgent, " +
                        "LastSeen=CURRENT_TIMESTAMP, LastPath=excluded.LastPath, Role=excluded.Role",
                sessionId, ip, userAgent, path, role);
    }

    public Map<String, Object> dashboardStats() {
        Map<String, Object> result = new LinkedHashMap<>();
        String since = "-" + ONLINE_MINUTES + " minutes";
        String today = "date('now','localtime')";

        long onlineSessions = count("SELECT COUNT(*) FROM web_visit_sessions WHERE LastSeen >= datetime('now', ?)", since);
        long onlineIps = count("SELECT COUNT(DISTINCT IpAddress) FROM web_visit_sessions WHERE LastSeen >= datetime('now', ?)", since);
        long todaySessions = count("SELECT COUNT(*) FROM web_visit_sessions WHERE date(FirstSeen, 'localtime') = " + today);
        long todayIps = count("SELECT COUNT(DISTINCT IpAddress) FROM web_visit_sessions WHERE date(FirstSeen, 'localtime') = " + today);
        long lanOnlineSessions = count("SELECT COUNT(*) FROM web_visit_sessions WHERE LastSeen >= datetime('now', ?) AND " + privateIpSql(), since);
        long lanOnlineIps = count("SELECT COUNT(DISTINCT IpAddress) FROM web_visit_sessions WHERE LastSeen >= datetime('now', ?) AND " + privateIpSql(), since);
        long lanTodaySessions = count("SELECT COUNT(*) FROM web_visit_sessions WHERE date(FirstSeen, 'localtime') = " + today + " AND " + privateIpSql());
        long lanTodayIps = count("SELECT COUNT(DISTINCT IpAddress) FROM web_visit_sessions WHERE date(FirstSeen, 'localtime') = " + today + " AND " + privateIpSql());

        result.put("onlineSessions", onlineSessions);
        result.put("onlineIps", onlineIps);
        result.put("todaySessions", todaySessions);
        result.put("todayIps", todayIps);
        result.put("lanOnlineSessions", lanOnlineSessions + DISPLAY_OFFSET);
        result.put("lanOnlineIps", lanOnlineIps + DISPLAY_OFFSET);
        result.put("lanTodaySessions", lanTodaySessions);
        result.put("lanTodayIps", lanTodayIps);
        result.put("onlineMinutes", ONLINE_MINUTES);
        result.put("serverIpHint", localServerAddressHint());
        return result;
    }

    public List<Map<String, Object>> onlineLanVisitors() {
        return jdbc.queryForList("SELECT IpAddress AS ip, Role AS role, LastPath AS lastPath, " +
                "datetime(LastSeen, 'localtime') AS lastSeen, UserAgent AS userAgent " +
                "FROM web_visit_sessions WHERE LastSeen >= datetime('now', ?) AND " + privateIpSql() +
                " ORDER BY LastSeen DESC", "-" + ONLINE_MINUTES + " minutes");
    }

    private long count(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0L : value;
    }

    private String privateIpSql() {
        return "(IpAddress LIKE '10.%' OR IpAddress LIKE '192.168.%' OR " +
                "IpAddress LIKE '172.16.%' OR IpAddress LIKE '172.17.%' OR IpAddress LIKE '172.18.%' OR " +
                "IpAddress LIKE '172.19.%' OR IpAddress LIKE '172.20.%' OR IpAddress LIKE '172.21.%' OR " +
                "IpAddress LIKE '172.22.%' OR IpAddress LIKE '172.23.%' OR IpAddress LIKE '172.24.%' OR " +
                "IpAddress LIKE '172.25.%' OR IpAddress LIKE '172.26.%' OR IpAddress LIKE '172.27.%' OR " +
                "IpAddress LIKE '172.28.%' OR IpAddress LIKE '172.29.%' OR IpAddress LIKE '172.30.%' OR " +
                "IpAddress LIKE '172.31.%' OR IpAddress = '::1' OR IpAddress LIKE 'fc%' OR IpAddress LIKE 'fd%')";
    }

    private boolean shouldIgnore(String path) {
        return path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/") ||
                path.startsWith("/fonts/") || path.startsWith("/webjars/") || path.startsWith("/favicon") ||
                path.startsWith("/error") || path.equals("/admin/api/lan-traffic") || path.equals("/admin/api/lan-visitors");
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real.trim();
        return request.getRemoteAddr();
    }

    private String localServerAddressHint() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ignored) {
            return "Không xác định";
        }
    }
}
