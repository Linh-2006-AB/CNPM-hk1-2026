package com.example.adminwebjava.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.example.adminwebjava.services.LanVisitorTrackingService;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;
    private final LanVisitorTrackingService lanVisitorTrackingService;
    public WebMvcConfig(AuthInterceptor authInterceptor, LanVisitorTrackingService lanVisitorTrackingService) {
        this.authInterceptor = authInterceptor;
        this.lanVisitorTrackingService = lanVisitorTrackingService;
    }
    @Override
    public void addInterceptors(InterceptorRegistry registry) { registry.addInterceptor(lanVisitorTrackingService);
        registry.addInterceptor(authInterceptor); }
}
