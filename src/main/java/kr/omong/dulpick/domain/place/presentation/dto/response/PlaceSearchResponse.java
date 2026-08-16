package kr.omong.dulpick.domain.place.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.place.application.PlaceSearchResult;
import kr.omong.dulpick.domain.place.domain.DulpickPlaceCategory;

import java.math.BigDecimal;

public record PlaceSearchResponse(
        @Schema(description = "Kakao 장소 검색 결과의 고유 ID. 장소 저장 요청에 사용합니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        String kakaoPlaceId,
        @Schema(description = "Kakao 장소 검색 결과의 장소명", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(description = "Kakao 지번 주소", requiredMode = Schema.RequiredMode.REQUIRED)
        String address,
        @Schema(description = "Kakao 도로명 주소. 제공되지 않으면 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String roadAddress,
        @Schema(description = "WGS84 기준 위도. 제공되지 않으면 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal latitude,
        @Schema(description = "WGS84 기준 경도. 제공되지 않으면 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal longitude,
        @Schema(description = "Kakao가 제공한 원본 카테고리 경로. 원본 값을 그대로 보존하며 없으면 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String category,
        @Schema(
                description = "둘픽 화면에서 사용하는 장소 분류입니다.",
                allowableValues = {"맛집", "카페", "놀거리", "쇼핑", "생활 편의", "관광", "숙박"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String categoryName
) {

    public static PlaceSearchResponse from(PlaceSearchResult result) {
        return new PlaceSearchResponse(
                result.kakaoPlaceId(),
                result.name(),
                result.address(),
                result.roadAddress(),
                result.latitude(),
                result.longitude(),
                result.category(),
                DulpickPlaceCategory.fromKakao(
                        result.categoryGroupCode(),
                        result.category()
                ).getDisplayName()
        );
    }
}
