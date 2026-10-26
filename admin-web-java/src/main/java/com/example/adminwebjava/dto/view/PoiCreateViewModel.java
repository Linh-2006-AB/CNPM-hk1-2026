package com.example.adminwebjava.dto.view;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoiCreateViewModel {

    private Integer id;

    @NotNull(message = "Tọa độ vĩ độ không được để trống")
    private BigDecimal latitude = new BigDecimal("10.762622");

    @NotNull(message = "Tọa độ kinh độ không được để trống")
    private BigDecimal longitude = new BigDecimal("106.660172");

    @NotNull(message = "Bán kính không được để trống")
    private Integer radius = 50;

    @NotBlank(message = "Tên địa điểm không được để trống")
    private String name = "";

    @NotBlank(message = "Mô tả ngắn không được để trống")
    private String shortDescription = "";

    @NotBlank(message = "Mô tả đầy đủ không được để trống")
    private String fullDescription = "";

    private String languageCode = "vi";

    private MultipartFile coverImage;
    private MultipartFile sourceAudio;
    private MultipartFile sourceVideo;
}