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

    @Test
    void keepsConvenienceAsDefaultButReportsUnclassifiedInput() {
        assertThat(DulpickPlaceCategory.fromKakao(null, "분류되지 않은 장소")).isEqualTo(
                DulpickPlaceCategory.CONVENIENCE
        );
        assertThat(DulpickPlaceCategory.isFallback(null, "분류되지 않은 장소")).isTrue();
        assertThat(DulpickPlaceCategory.isFallback(null, "여행 > 공원")).isFalse();
        assertThat(DulpickPlaceCategory.isFallback("CE7", "분류되지 않은 장소")).isFalse();
    }

    @Test
    void classifiesCommonKakaoPathsThatDoNotHaveAGroupCode() {
        assertThat(DulpickPlaceCategory.fromKakao(null, "가정,생활 > 미용 > 화장품"))
                .isEqualTo(DulpickPlaceCategory.SHOPPING);
        assertThat(DulpickPlaceCategory.fromKakao(null, "문화,예술 > 도서 > 서점"))
                .isEqualTo(DulpickPlaceCategory.SHOPPING);
        assertThat(DulpickPlaceCategory.fromKakao(null, "문화,예술 > 음악 > 음악감상실"))
                .isEqualTo(DulpickPlaceCategory.ENTERTAINMENT);
        assertThat(DulpickPlaceCategory.fromKakao(null, "가정,생활 > 생활용품점 > 인테리어장식판매"))
                .isEqualTo(DulpickPlaceCategory.SHOPPING);
        assertThat(DulpickPlaceCategory.fromKakao(null, "가정,생활 > 패션 > 의류판매"))
                .isEqualTo(DulpickPlaceCategory.SHOPPING);
        assertThat(DulpickPlaceCategory.fromKakao(null, "가정,생활 > 식품판매 > 아이스크림판매"))
                .isEqualTo(DulpickPlaceCategory.CAFE);
    }
}
