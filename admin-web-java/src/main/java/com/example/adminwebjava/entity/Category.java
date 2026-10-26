package com.example.adminwebjava.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "IconUrl")
    private String iconUrl;

    @Column(nullable = false)
    private String status = "active";

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    private List<CategoryTranslation> translations = new ArrayList<>();
}
