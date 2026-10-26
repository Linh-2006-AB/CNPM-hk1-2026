package com.example.adminwebjava.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="visitor_playback_logs")
public class VisitorPlaybackLog {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Integer id;
 @Column(name="TouristId") private Integer touristId;
 // Map the existing TouristId column as a read-only relationship for Tourist.playbackLogs.
 @ManyToOne(fetch = FetchType.LAZY)
 @JoinColumn(name="TouristId", referencedColumnName="id", insertable=false, updatable=false)
 private Tourist tourist;
 @Column(name="PoiId") private Integer poiId;
 private String languageCode; private String audioUrl; private LocalDateTime playedAt;
 public Integer getId(){return id;} public Integer getTouristId(){return touristId;} public Integer getPoiId(){return poiId;}
 public String getLanguageCode(){return languageCode;} public String getAudioUrl(){return audioUrl;} public LocalDateTime getPlayedAt(){return playedAt;}
}
