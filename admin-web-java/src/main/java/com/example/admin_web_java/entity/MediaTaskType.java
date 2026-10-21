package com.example.adminwebjava.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "SupportedLanguages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupportedLanguage {
    @Id
    @Size(max = 10)
    @Column(length = 10, nullable = false)
    private String languageCode;

    @Size(max = 100)
    @Column(length = 100, nullable = false)
    private String languageName = "";

    @Size(max = 100)
    @Column(length = 100)
    private String edgeTtsVoice = "";

    @Column(nullable = false)
    private Boolean isActive = true;
}