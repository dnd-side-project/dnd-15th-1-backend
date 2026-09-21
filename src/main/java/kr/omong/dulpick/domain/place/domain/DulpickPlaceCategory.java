package kr.omong.dulpick.domain.place.domain;

import java.util.List;
import java.util.Locale;

public enum DulpickPlaceCategory {
    RESTAURANT("맛집"),
    CAFE("카페"),
    ENTERTAINMENT("놀거리"),
    SHOPPING("쇼핑"),
    CONVENIENCE("생활 편의"),
    TOURISM("관광"),
    ACCOMMODATION("숙박");

    private final String displayName;

    private static final List<KakaoCategoryGroup> KAKAO_CATEGORY_GROUPS = List.of(
            new KakaoCategoryGroup("FD6", "음식점"),
            new KakaoCategoryGroup("CE7", "카페"),
            new KakaoCategoryGroup("CT1", "문화시설"),
            new KakaoCategoryGroup("MT1", "대형마트"),
            new KakaoCategoryGroup("CS2", "편의점"),
            new KakaoCategoryGroup("AT4", "관광명소"),
            new KakaoCategoryGroup("AD5", "숙박"),
            new KakaoCategoryGroup("PS3", "어린이집"),
            new KakaoCategoryGroup("SC4", "학교"),
            new KakaoCategoryGroup("AC5", "학원"),
            new KakaoCategoryGroup("PK6", "주차장"),
            new KakaoCategoryGroup("OL7", "주유소"),
            new KakaoCategoryGroup("SW8", "지하철역"),
            new KakaoCategoryGroup("BK9", "은행"),
            new KakaoCategoryGroup("AG2", "관공서"),
            new KakaoCategoryGroup("PO3", "병원"),
            new KakaoCategoryGroup("PM9", "약국")
    );

    DulpickPlaceCategory(String displayName) {
        this.displayName = displayName;
    }

    public static DulpickPlaceCategory fromKakao(
            String categoryGroupCode,
            String kakaoCategory
    ) {
        DulpickPlaceCategory groupCategory = fromGroupCode(categoryGroupCode);
        return groupCategory == null ? fromCategoryPath(kakaoCategory) : groupCategory;
    }

    public static boolean isFallback(String categoryGroupCode, String kakaoCategory) {
        return fromGroupCode(categoryGroupCode) == null
                && fromCategoryPathOrNull(kakaoCategory) == null;
    }

    public static List<KakaoCategoryGroup> kakaoCategoryGroups() {
        return KAKAO_CATEGORY_GROUPS;
    }

    public static boolean isSupportedGroupCode(String categoryGroupCode) {
        if (categoryGroupCode == null || categoryGroupCode.isBlank()) {
            return true;
        }
        String normalized = categoryGroupCode.strip().toUpperCase(Locale.ROOT);
        return KAKAO_CATEGORY_GROUPS.stream()
                .map(KakaoCategoryGroup::code)
                .anyMatch(normalized::equals);
    }

    public String getDisplayName() {
        return displayName;
    }

    private static DulpickPlaceCategory fromGroupCode(String categoryGroupCode) {
        if (categoryGroupCode == null || categoryGroupCode.isBlank()) {
            return null;
        }
        return switch (categoryGroupCode.strip().toUpperCase(Locale.ROOT)) {
            case "FD6" -> RESTAURANT;
            case "CE7" -> CAFE;
            case "CT1" -> ENTERTAINMENT;
            case "MT1", "CS2" -> SHOPPING;
            case "AT4" -> TOURISM;
            case "AD5" -> ACCOMMODATION;
            case "PS3", "SC4", "AC5", "PK6", "OL7", "SW8", "BK9", "AG2", "PO3", "PM9" ->
                    CONVENIENCE;
            default -> null;
        };
    }

    private static DulpickPlaceCategory fromCategoryPath(String kakaoCategory) {
        DulpickPlaceCategory category = fromCategoryPathOrNull(kakaoCategory);
        return category == null ? CONVENIENCE : category;
    }

    private static DulpickPlaceCategory fromCategoryPathOrNull(String kakaoCategory) {
        String category = normalize(kakaoCategory);
        if (containsAny(category, "카페", "커피", "디저트", "아이스크림")) {
            return CAFE;
        }
        if (containsAny(category, "음식점", "한식", "중식", "일식", "양식", "분식", "주점", "술집")) {
            return RESTAURANT;
        }
        if (containsAny(category, "숙박", "호텔", "모텔", "펜션", "게스트하우스", "캠핑")) {
            return ACCOMMODATION;
        }
        if (containsAny(category, "관광", "명소", "여행", "공원", "해수욕장", "자연")) {
            return TOURISM;
        }
        if (containsAny(
                category,
                "마트", "편의점", "쇼핑", "백화점", "아울렛", "시장", "화장품", "미용",
                "도서", "서점", "문구", "완구", "생활용품", "인테리어", "패션", "의류",
                "꽃집", "꽃배달", "식품판매"
        )) {
            return SHOPPING;
        }
        if (containsAny(
                category,
                "문화", "공연", "극장", "영화관", "미술관", "박물관", "전시", "놀이", "오락",
                "방탈출", "볼링", "노래방", "스포츠", "레저", "음악"
        )) {
            return ENTERTAINMENT;
        }
        return null;
    }

    private static String normalize(String category) {
        return category == null
                ? ""
                : category.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private static boolean containsAny(String category, String... keywords) {
        for (String keyword : keywords) {
            if (category.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public record KakaoCategoryGroup(String code, String displayName) {
    }
}
