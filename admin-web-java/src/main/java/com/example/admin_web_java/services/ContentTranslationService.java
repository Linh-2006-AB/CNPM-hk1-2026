package com.example.adminwebjava.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.adminwebjava.entity.*;
import com.example.adminwebjava.repository.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentTranslationService {

    private final PoiRepository poiRepository;
    private final PoiTranslationRepository poiTranslationRepository;
    private final TourRepository tourRepository;
    private final TourTranslationRepository tourTranslationRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryTranslationRepository categoryTranslationRepository;
    private final SupportedLanguageRepository supportedLanguageRepository;
    
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record TranslationSummary(int tours, int categories, int pois) {
        public int getTotal() {
            return pois + tours + categories;
        }
    }

    @Transactional
    public TranslationSummary translateMissingContentForLanguage(String languageCode) {
        String code = normalizeCode(languageCode);

        if (code.isBlank() || "vi".equalsIgnoreCase(code)) {
            return new TranslationSummary(0, 0, 0);
        }

        int poiCount = translateMissingPois(code);
        int tourCount = translateMissingTours(code);
        int categoryCount = translateMissingCategories(code);

        return new TranslationSummary(tourCount, categoryCount, poiCount);
    }

    @Transactional
    public TranslationSummary translateMissingContentForAllActiveLanguages() {
        List<SupportedLanguage> languages = getTargetLanguages(null);
        int totalPois = 0;
        int totalTours = 0;
        int totalCategories = 0;

        for (SupportedLanguage language : languages) {
            totalPois += translateMissingPoisForLanguage(language);
            totalTours += translateMissingToursForLanguage(language);
            totalCategories += translateMissingCategoriesForLanguage(language);
        }

        return new TranslationSummary(totalTours, totalCategories, totalPois);
    }

    @Transactional
    public int translateMissingTours(String languageCode) {
        List<SupportedLanguage> languages = getTargetLanguages(languageCode);
        int total = 0;

        for (SupportedLanguage language : languages) {
            total += translateMissingToursForLanguage(language);
        }

        return total;
    }

    @Transactional
    public int translateMissingCategories(String languageCode) {
        List<SupportedLanguage> languages = getTargetLanguages(languageCode);
        int total = 0;

        for (SupportedLanguage language : languages) {
            total += translateMissingCategoriesForLanguage(language);
        }

        return total;
    }

    @Transactional
    public int translateMissingPois(String languageCode) {
        List<SupportedLanguage> languages = getTargetLanguages(languageCode);
        int total = 0;

        for (SupportedLanguage language : languages) {
            total += translateMissingPoisForLanguage(language);
        }

        return total;
    }

    private int translateMissingPoisForLanguage(SupportedLanguage language) {
        String targetCode = normalizeCode(language.getLanguageCode());

        List<Poi> pois = poiRepository.findPoisMissingLanguage(targetCode);

        List<PoiSourceItem> items = pois.stream()
                .map(poi -> {
                    PoiTranslation source = pickSourceTranslation(poi.getTranslations());
                    if (source == null) return null;
                    return new PoiSourceItem(
                            poi.getId(),
                            source.getName(),
                            source.getShortDescription() != null ? source.getShortDescription() : "",
                            source.getFullDescription() != null ? source.getFullDescription() : ""
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (items.isEmpty()) {
            return 0;
        }

        try {
            String prompt =
                    "Bạn là biên dịch viên nội dung du lịch cho hệ thống tour guide.\n" +
                    "Hãy dịch danh sách POI sang ngôn ngữ đích: " + language.getLanguageName() + " (" + targetCode + ").\n" +
                    "Giữ nguyên id. Dịch tự nhiên, đúng văn phong du lịch, không bịa thêm dữ kiện mới.\n" +
                    "Quy tắc dịch tên POI:\n" +
                    "- Field name cũng phải được dịch sang ngôn ngữ đích. Với địa danh nổi tiếng, dùng tên bản địa/tên phổ biến trong ngôn ngữ đó.\n" +
                    "- Không giữ nguyên tiếng Anh/tiếng Việt cho name nếu có bản dịch phổ biến.\n" +
                    "- Nếu là thương hiệu/tên quán/tên riêng không nên dịch, hãy giữ tên gốc.\n" +
                    "- Nếu không chắc, dùng tên đã dịch và thêm tên gốc trong ngoặc.\n" +
                    "Trả về DUY NHẤT JSON array, không markdown, không giải thích.\n" +
                    "Schema bắt buộc: [{\"id\":1,\"name\":\"...\",\"shortDescription\":\"...\",\"fullDescription\":\"...\"}]\n" +
                    "Dữ liệu nguồn:\n" +
                    objectMapper.writeValueAsString(items);

            String response = geminiService.generateText(prompt);
            List<PoiTranslatedItem> translatedList = parseArray(response, new TypeReference<List<PoiTranslatedItem>>() {});
            
            Map<Integer, PoiTranslatedItem> translatedItems = translatedList.stream()
                    .filter(item -> item.getId() > 0 && item.getName() != null && !item.getName().isBlank())
                    .collect(Collectors.toMap(PoiTranslatedItem::getId, item -> item, (existing, replacement) -> existing));

            Set<Integer> existingSet = new HashSet<>(poiTranslationRepository.findPoiIdsByLanguageCode(targetCode));
            List<PoiTranslation> newItems = new ArrayList<>();

            for (PoiSourceItem source : items) {
                if (existingSet.contains(source.id())) {
                    continue;
                }

                PoiTranslatedItem translated = translatedItems.get(source.id());
                if (translated == null) {
                    continue;
                }

                PoiTranslation translation = new PoiTranslation();
                translation.setPoiId(source.id());
                translation.setLanguageCode(targetCode);
                translation.setName(translated.getName().trim());
                translation.setShortDescription((translated.getShortDescription() == null || translated.getShortDescription().isBlank()) ? null : translated.getShortDescription().trim());
                translation.setFullDescription((translated.getFullDescription() == null || translated.getFullDescription().isBlank()) ? null : translated.getFullDescription().trim());
                translation.setTtsScript((translated.getFullDescription() == null || translated.getFullDescription().isBlank()) ? null : translated.getFullDescription().trim());
                translation.setUpdatedAt(LocalDateTime.now());

                newItems.add(translation);
            }

            if (newItems.isEmpty()) {
                return 0;
            }

            poiTranslationRepository.saveAll(newItems);
            return newItems.size();
        } catch (Exception e) {
            log.error("Lỗi dịch POI cho ngôn ngữ {}: {}", targetCode, e.getMessage(), e);
            return 0;
        }
    }

    private int translateMissingToursForLanguage(SupportedLanguage language) {
        String targetCode = normalizeCode(language.getLanguageCode());

        List<Tour> tours = tourRepository.findToursMissingLanguage(targetCode);

        List<TourSourceItem> items = tours.stream()
                .map(tour -> {
                    TourTranslation source = pickSourceTranslation(tour.getTranslations());
                    if (source == null) return null;
                    return new TourSourceItem(tour.getId(), source.getTitle(), source.getDescription() != null ? source.getDescription() : "");
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (items.isEmpty()) {
            return 0;
        }

        try {
            String prompt =
                    "Bạn là biên dịch viên nội dung du lịch cho hệ thống tour guide.\n" +
                    "Hãy dịch danh sách TOUR sang ngôn ngữ đích: " + language.getLanguageName() + " (" + targetCode + ").\n" +
                    "Giữ nguyên id. Dịch tự nhiên, đúng văn phong du lịch, không bịa thêm dữ kiện mới.\n" +
                    "Trả về DUY NHẤT JSON array, không markdown, không giải thích.\n" +
                    "Schema bắt buộc: [{\"id\":1,\"title\":\"...\",\"description\":\"...\"}]\n" +
                    "Dữ liệu nguồn:\n" +
                    objectMapper.writeValueAsString(items);

            String response = geminiService.generateText(prompt);
            List<TourTranslatedItem> translatedList = parseArray(response, new TypeReference<List<TourTranslatedItem>>() {});

            Map<Integer, TourTranslatedItem> translatedItems = translatedList.stream()
                    .filter(item -> item.getId() > 0 && item.getTitle() != null && !item.getTitle().isBlank())
                    .collect(Collectors.toMap(TourTranslatedItem::getId, item -> item, (existing, replacement) -> existing));

            Set<Integer> existingSet = new HashSet<>(tourTranslationRepository.findTourIdsByLanguageCode(targetCode));
            List<TourTranslation> newItems = new ArrayList<>();

            for (TourSourceItem source : items) {
                if (existingSet.contains(source.id())) {
                    continue;
                }

                TourTranslatedItem translated = translatedItems.get(source.id());
                if (translated == null) {
                    continue;
                }

                TourTranslation translation = new TourTranslation();
                translation.setTourId(source.id());
                translation.setLanguageCode(targetCode);
                translation.setTitle(translated.getTitle().trim());
                translation.setDescription((translated.getDescription() == null || translated.getDescription().isBlank()) ? null : translated.getDescription().trim());

                newItems.add(translation);
            }

            if (newItems.isEmpty()) {
                return 0;
            }

            tourTranslationRepository.saveAll(newItems);
            return newItems.size();
        } catch (Exception e) {
            log.error("Lỗi dịch Tour cho ngôn ngữ {}: {}", targetCode, e.getMessage(), e);
            return 0;
        }
    }

    private int translateMissingCategoriesForLanguage(SupportedLanguage language) {
        String targetCode = normalizeCode(language.getLanguageCode());

        List<Category> categories = categoryRepository.findCategoriesMissingLanguage(targetCode);

        List<CategorySourceItem> items = categories.stream()
                .map(category -> {
                    CategoryTranslation source = pickSourceTranslation(category.getTranslations());
                    if (source == null) return null;
                    return new CategorySourceItem(category.getId(), source.getName());
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (items.isEmpty()) {
            return 0;
        }

        try {
            String prompt =
                    "Bạn là biên dịch viên nội dung du lịch cho hệ thống tour guide.\n" +
                    "Hãy dịch danh sách DANH MỤC sang ngôn ngữ đích: " + language.getLanguageName() + " (" + targetCode + ").\n" +
                    "Giữ nguyên id. Tên danh mục phải ngắn gọn, tự nhiên, phù hợp giao diện app/web.\n" +
                    "Trả về DUY NHẤT JSON array, không markdown, không giải thích.\n" +
                    "Schema bắt buộc: [{\"id\":1,\"name\":\"...\"}]\n" +
                    "Dữ liệu nguồn:\n" +
                    objectMapper.writeValueAsString(items);

            String response = geminiService.generateText(prompt);
            List<CategoryTranslatedItem> translatedList = parseArray(response, new TypeReference<List<CategoryTranslatedItem>>() {});

            Map<Integer, CategoryTranslatedItem> translatedItems = translatedList.stream()
                    .filter(item -> item.getId() > 0 && item.getName() != null && !item.getName().isBlank())
                    .collect(Collectors.toMap(CategoryTranslatedItem::getId, item -> item, (existing, replacement) -> existing));

            Set<Integer> existingSet = new HashSet<>(categoryTranslationRepository.findCategoryIdsByLanguageCode(targetCode));
            List<CategoryTranslation> newItems = new ArrayList<>();

            for (CategorySourceItem source : items) {
                if (existingSet.contains(source.id())) {
                    continue;
                }

                CategoryTranslatedItem translated = translatedItems.get(source.id());
                if (translated == null) {
                    continue;
                }

                CategoryTranslation translation = new CategoryTranslation();
                translation.setCategoryId(source.id());
                translation.setLanguageCode(targetCode);
                translation.setName(translated.getName().trim());

                newItems.add(translation);
            }

            if (newItems.isEmpty()) {
                return 0;
            }

            categoryTranslationRepository.saveAll(newItems);
            return newItems.size();
        } catch (Exception e) {
            log.error("Lỗi dịch Danh mục cho ngôn ngữ {}: {}", targetCode, e.getMessage(), e);
            return 0;
        }
    }

    private List<SupportedLanguage> getTargetLanguages(String languageCode) {
        String normalized = normalizeCode(languageCode);

        if (!normalized.isBlank()) {
            return supportedLanguageRepository.findByIsActiveTrueAndLanguageCodeNotAndLanguageCode("vi", normalized);
        }

        return supportedLanguageRepository.findByIsActiveTrueAndLanguageCodeNotOrderByLanguageCodeAsc("vi");
    }

    private static PoiTranslation pickSourceTranslation(Collection<PoiTranslation> translations) {
        if (translations == null) return null;
        return translations.stream()
                .filter(item -> "vi".equalsIgnoreCase(item.LanguageCode) && item.getName() != null && !item.getName().isBlank())
                .findFirst()
                .orElseGet(() -> translations.stream()
                        .filter(item -> "en".equalsIgnoreCase(item.LanguageCode) && item.getName() != null && !item.getName().isBlank())
                        .findFirst()
                        .orElseGet(() -> translations.stream()
                                .filter(item -> item.getName() != null && !item.getName().isBlank())
                                .findFirst()
                                .orElse(null)));
    }

    private static TourTranslation pickSourceTranslation(Collection<TourTranslation> translations) {
        if (translations == null) return null;
        return translations.stream()
                .filter(item -> "vi".equalsIgnoreCase(item.getLanguageCode()) && item.getTitle() != null && !item.getTitle().isBlank())
                .findFirst()
                .orElseGet(() -> translations.stream()
                        .filter(item -> "en".equalsIgnoreCase(item.getLanguageCode()) && item.getTitle() != null && !item.getTitle().isBlank())
                        .findFirst()
                        .orElseGet(() -> translations.stream()
                                .filter(item -> item.getTitle() != null && !item.getTitle().isBlank())
                                .findFirst()
                                .orElse(null)));
    }

    private static CategoryTranslation pickSourceTranslation(Collection<CategoryTranslation> translations) {
        if (translations == null) return null;
        return translations.stream()
                .filter(item -> "vi".equalsIgnoreCase(item.getLanguageCode()) && item.getName() != null && !item.getName().isBlank())
                .findFirst()
                .orElseGet(() -> translations.stream()
                        .filter(item -> "en".equalsIgnoreCase(item.getLanguageCode()) && item.getName() != null && !item.getName().isBlank())
                        .findFirst()
                        .orElseGet(() -> translations.stream()
                                .filter(item -> item.getName() != null && !item.getName().isBlank())
                                .findFirst()
                                .orElse(null)));
    }

    private <T> List<T> parseArray(String text, TypeReference<List<T>> typeReference) {
        try {
            String json = extractFirstJsonArray(text);
            List<T> items = objectMapper.readValue(json, typeReference);
            return items != null ? items : Collections.emptyList();
        } catch (Exception e) {
            log.error("Không thể parse JSON từ phản hồi của Gemini: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private static String extractFirstJsonArray(String text) {
        String cleaned = (text == null ? "" : text)
                .replaceAll("(?i)```json", "")
                .replaceAll("```", "")
                .trim();

        int start = cleaned.indexOf('[');
        if (start < 0) {
            throw new IllegalStateException("Gemini không trả về JSON array hợp lệ.");
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = start; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (inString) {
                continue;
            }

            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
            }

            if (depth == 0) {
                return cleaned.substring(start, i + 1);
            }
        }

        throw new IllegalStateException("Gemini trả về JSON array chưa hoàn chỉnh.");
    }

    private static String normalizeCode(String code) {
        return (code == null) ? "" : code.trim().toLowerCase(Locale.ROOT);
    }

    // Inner Records and DTOs
    private record PoiSourceItem(int id, String name, String shortDescription, String fullDescription) {}
    private record TourSourceItem(int id, String title, String description) {}
    private record CategorySourceItem(int id, String name) {}

    @Getter
    @Setter
    private static class PoiTranslatedItem {
        private int id;
        private String name = "";
        private String shortDescription;
        private String fullDescription;
    }

    @Getter
    @Setter
    private static class TourTranslatedItem {
        private int id;
        private String title = "";
        private String description;
    }

    @Getter
    @Setter
    private static class CategoryTranslatedItem {
        private int id;
        private String name = "";
    }
}