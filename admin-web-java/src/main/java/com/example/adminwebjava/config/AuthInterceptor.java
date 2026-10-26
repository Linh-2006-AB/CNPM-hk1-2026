package com.example.adminwebjava.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        if (!path.startsWith("/admin") && !path.startsWith("/owner")) return true;
        HttpSession session = request.getSession(false);
        String role = session == null ? null : (String) session.getAttribute("authRole");
        boolean allowed = path.startsWith("/admin") ? "Admin".equalsIgnoreCase(role) : "Owner".equalsIgnoreCase(role);
        if (allowed) return true;
        response.sendRedirect(request.getContextPath() + "/auth/login");
        return false;
    }
}
