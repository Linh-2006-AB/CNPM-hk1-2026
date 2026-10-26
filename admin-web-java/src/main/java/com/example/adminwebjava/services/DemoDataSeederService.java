package com.example.adminwebjava.services;

import com.example.adminwebjava.entity.PoiTranslation;
import com.example.adminwebjava.entity.SupportedLanguage;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DemoDataSeederService {

    // Helper: Chuẩn hóa chuỗi về dạng Slug
    private static String toSafeSlug(String input) {
        if (input == null) return "";
        return input.toLowerCase()
                .replaceAll("[áàảãạâấầẩẫậăắằẳẵặ]", "a")
                .replaceAll("[éèẻẽẹêếềểễệ]", "e")
                .replaceAll("[iíìỉĩị]", "i")
                .replaceAll("[óòỏõọôốồổỗộơớờởỡợ]", "o")
                .replaceAll("[úùủũụưứừửữự]", "u")
                .replaceAll("[ýỳỷỹỵ]", "y")
                .replaceAll("[đ]", "d")
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("[\\s-]+", "-")
                .replaceAll("^-|-$", "");
    }

    // 1. Ánh xạ tên POI theo ngôn ngữ
    private static String getLocalizedPoiName(String languageCode, String fallbackName) {
        String code = languageCode == null ? "" : languageCode.trim().toLowerCase();
        String key = toSafeSlug(fallbackName);

        switch (key) {
            case "dinh-doc-lap":
            case "independence-palace":
                return pickName(code, "Dinh Độc Lập", "Independence Palace", "独立宫", "独立宮殿", "독립궁", "Palais de l’Indépendance");

            case "nha-tho-duc-ba-sai-gon":
            case "notre-dame-cathedral-of-saigon":
                return pickName(code, "Nhà thờ Đức Bà Sài Gòn", "Notre Dame Cathedral of Saigon", "西贡圣母大教堂", "サイゴン大教会", "사이공 노트르담 대성당", "Cathédrale Notre-Dame de Saïgon");

            case "buu-dien-trung-tam-sai-gon":
            case "saigon-central-post-office":
                return pickName(code, "Bưu điện Trung tâm Sài Gòn", "Saigon Central Post Office", "西贡中央邮局", "サイゴン中央郵便局", "사이공 중앙우체국", "Poste centrale de Saïgon");

            case "cho-ben-thanh":
            case "ben-thanh-market":
                return pickName(code, "Chợ Bến Thành", "Ben Thanh Market", "滨城市场", "ベンタイン市場", "ベンタン市場", "Marché Bến Thành");

            default:
                return fallbackName;
        }
    }

    // 2. Chọn tên theo mã ngôn ngữ
    private static String pickName(String code, String vi, String en, String zh, String ja, String ko, String fr) {
        return switch (code) {
            case "vi" -> vi;
            case "zh" -> zh;
            case "ja" -> ja;
            case "ko" -> ko;
            case "fr" -> fr;
            default -> en; // Fallback sang Tiếng Anh
        };
    }

    // 3. Tạo bản dịch POI tự động
    public PoiTranslation createLocalizedPoiTranslation(SupportedLanguage language, PoiTranslation source, LocalDateTime now) {
        String sourceName = (source == null || source.getName() == null || source.getName().isBlank()) 
                            ? "POI" : source.getName().trim();
        String name = getLocalizedPoiName(language.getLanguageCode(), sourceName);

        String shortDescription = switch (language.getLanguageCode()) {
            case "en" -> "Audio guide for " + name + " in Ho Chi Minh City.";
            case "zh" -> name + " 的胡志明市语音导览。";
            case "ja" -> name + " のホーチミン市観光音声ガイドです。";
            case "ko" -> name + "의 호치민시 관광 음성 안내입니다.";
            default -> (source != null && source.getShortDescription() != null) 
                       ? source.getShortDescription() 
                       : "Thuyết minh tham quan cho " + name + ".";
        };

        String fullDescription = switch (language.getLanguageCode()) {
            case "en" -> (source != null && source.getFullDescription() != null && !source.getFullDescription().isBlank()) 
                         ? source.getFullDescription() 
                         : "This is " + name + "...";
            case "zh" -> "这里是 " + name + "，胡志明市的一个旅游景点...";
            case "ja" -> "ここは " + name + "、ホーチミン市の観光スポットです...";
            case "ko" -> "이곳은 " + name + ", 호치민시의 관광 명소입니다...";
            default -> (source != null && source.getFullDescription() != null) 
                       ? source.getFullDescription() 
                       : "Đây là " + name + ", một điểm tham quan tại Thành phố Hồ Chí Minh.";
        };

        PoiTranslation translation = new PoiTranslation();
        translation.setLanguageCode(language.getLanguageCode());
        translation.setName(name);
        translation.setShortDescription(shortDescription);
        translation.setFullDescription(fullDescription);
        translation.setTtsScript(fullDescription);
        translation.setAudioUrl(null);
        translation.setVideoUrl(null);
        translation.setAudioDuration(0);
        translation.setUpdatedAt(now);

        return translation;
    }
}