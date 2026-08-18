package kr.omong.dulpick.domain.place.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.place.application.PlaceSearchView;
import kr.omong.dulpick.domain.place.domain.DulpickPlaceCategory;
import kr.omong.dulpick.domain.place.domain.PlaceOwnershipStatus;

import java.math.BigDecimal;
import java.util.List;

public record PlaceSearchResponse(
        @Schema(
                description = "공용 DB에 존재하는 장소 ID. Kakao에만 존재하면 null입니다.",
                example = "101",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        Long placeId,
        @Schema(description = "Kakao 장소 검색 결과의 고유 ID. 장소 저장 요청에 사용합니다.", example = "18699959", requiredMode = Schema.RequiredMode.REQUIRED)
        String kakaoPlaceId,
        @Schema(description = "Kakao 장소 검색 결과의 장소명", example = "서울숲 카페", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(description = "Kakao 지번 주소", example = "서울특별시 성동구 성수동1가 685-700", requiredMode = Schema.RequiredMode.REQUIRED)
        String address,
        @Schema(description = "Kakao 도로명 주소. 제공되지 않으면 null입니다.", example = "서울특별시 성동구 서울숲2길 10", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String roadAddress,
        @Schema(description = "WGS84 기준 위도. 제공되지 않으면 null입니다.", example = "37.5446", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal latitude,
        @Schema(description = "WGS84 기준 경도. 제공되지 않으면 null입니다.", example = "127.0557", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal longitude,
        @Schema(description = "Kakao가 제공한 원본 카테고리 경로. 원본 값을 그대로 보존하며 없으면 null입니다.", example = "음식점 > 카페", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String category,
        @Schema(
                description = "둘픽 카테고리 코드",
                allowableValues = {
                        "RESTAURANT", "CAFE", "ENTERTAINMENT", "SHOPPING",
                        "CONVENIENCE", "TOURISM", "ACCOMMODATION"
                },
                example = "CAFE",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        DulpickPlaceCategory categoryCode,
        @Schema(
                description = "둘픽 화면에서 사용하는 장소 분류입니다.",
                allowableValues = {"맛집", "카페", "놀거리", "쇼핑", "생활 편의", "관광", "숙박"},
                example = "카페",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String categoryName,
        @Schema(description = "Kakao 전화번호. 제공되지 않으면 null입니다.", example = "02-1234-5678", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String phone,
        @Schema(description = "Kakao 장소 상세 URL. 제공되지 않으면 null입니다.", example = "https://place.map.kakao.com/18699959", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String kakaoPlaceUrl,
        @Schema(description = "현재 회원이 이 장소를 저장했는지 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean savedByMe,
        @Schema(
                description = "현재 활성 커플의 저장 주체. 아무도 저장하지 않았으면 null입니다.",
                allowableValues = {"MINE", "PARTNER", "TOGETHER"},
                example = "TOGETHER",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        PlaceOwnershipStatus ownershipStatus,
        @Schema(description = "대표 장소 이미지 URL", example = "https://example.com/place.jpg", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String thumbnailUrl,
        @ArraySchema(schema = @Schema(example = "https://example.com/place-detail.jpg"))
        @Schema(description = "대표 이미지를 제외한 장소 이미지 URL 목록", example = "[]", requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> imageUrls
) {

    public static PlaceSearchResponse from(PlaceSearchView view) {
        return new PlaceSearchResponse(
                view.placeId(),
                view.kakaoPlaceId(),
                view.name(),
                view.address(),
                view.roadAddress(),
                view.latitude(),
                view.longitude(),
                view.category(),
                view.categoryCode(),
                view.categoryCode().getDisplayName(),
                view.phone(),
                view.kakaoPlaceUrl(),
                view.savedByMe(),
                view.ownershipStatus(),
                view.thumbnailUrl(),
                view.imageUrls()
        );
    }
}
