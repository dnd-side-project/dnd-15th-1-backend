package kr.omong.dulpick.domain.place;

import kr.omong.dulpick.domain.place.domain.DulpickPlaceCategory;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DulpickPlaceCategoryTest {

    @Test
    void mapsEverySupportedKakaoGroupCode() {
        Map<String, String> expectedNames = Map.ofEntries(
                Map.entry("FD6", "맛집"),
                Map.entry("CE7", "카페"),
                Map.entry("CT1", "놀거리"),
                Map.entry("MT1", "쇼핑"),
                Map.entry("CS2", "쇼핑"),
                Map.entry("PS3", "생활 편의"),
                Map.entry("SC4", "생활 편의"),
                Map.entry("AC5", "생활 편의"),
                Map.entry("PK6", "생활 편의"),
                Map.entry("OL7", "생활 편의"),
                Map.entry("SW8", "생활 편의"),
                Map.entry("BK9", "생활 편의"),
                Map.entry("AG2", "생활 편의"),
                Map.entry("PO3", "생활 편의"),
                Map.entry("PM9", "생활 편의"),
                Map.entry("AT4", "관광"),
                Map.entry("AD5", "숙박")
        );

        expectedNames.forEach((code, expectedName) -> assertThat(
                DulpickPlaceCategory.fromKakao(code, "").getDisplayName()
        ).isEqualTo(expectedName));
    }

    @Test
    void classifiesExistingPlaceWithoutGroupCodeFromKakaoCategoryPath() {
        assertThat(DulpickPlaceCategory.fromKakao(
                null,
                "여행 > 공원 > 도시근린공원"
        ).getDisplayName()).isEqualTo("관광");
        assertThat(DulpickPlaceCategory.fromKakao(
                "",
                "가정,생활 > 목욕탕,사우나 > 찜질방"
        ).getDisplayName()).isEqualTo("생활 편의");
    }
}
