package com.example.adminwebjava.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="tour_translations") @Data @NoArgsConstructor @AllArgsConstructor
public class TourTranslation {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
 @Column(name="TourId", insertable=false, updatable=false) private Integer tourId;
 @Column(nullable=false, length=10) private String languageCode="vi";
 @Column(nullable=false) private String title="";
 @Column(columnDefinition="TEXT") private String description;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="TourId") private Tour tour;
}
