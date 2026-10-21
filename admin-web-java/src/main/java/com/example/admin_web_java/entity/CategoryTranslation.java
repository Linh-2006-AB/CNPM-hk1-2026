package com.example.adminwebjava.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CategoryTranslations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTranslation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "CategoryId", insertable = false, updatable = false)
    private Integer categoryId;

    @Column(nullable = false, length = 10)
    private String languageCode = "vi";

    @Column(nullable = false)
    private String name = "";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CategoryId")
    private Category category;
}