package com.example.adminwebjava;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
    "com.example.admin_web_java",
    "com.example.adminwebjava",
    "com.adminweb.services",
    "vn.versa.tourguide"
})
@EntityScan(basePackages = {
    "com.example.adminwebjava.entity"
})
@EnableJpaRepositories(basePackages = {
    "com.example.adminwebjava.repository",
    "com.example.admin_web_java.repository"
})
public class AdminWebJavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminWebJavaApplication.class, args);
    }
}
