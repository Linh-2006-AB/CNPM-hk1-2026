package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LanguageDto {
    private String code = "";
    private String name = "";
    private String nativeName = "";
    private String edgeTtsVoice = "";
}