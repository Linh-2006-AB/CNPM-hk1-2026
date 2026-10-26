package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TouristProfileDto {
    private Integer id;
    private String email = "";
    private String fullName = "";
    private LocalDateTime createdAt;
}