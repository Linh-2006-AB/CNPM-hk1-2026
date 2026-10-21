package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoiTranslationDto {
    private String languageCode = "";
    private String languageName = "";
    private String name = "";
    private String shortDescription = "";
    private String fullDescription = "";
    private String audioUrl;
    private String videoUrl;
}