package kr.omong.dulpick.domain.place.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.place.application.MemberPlaceView;
import kr.omong.dulpick.domain.place.domain.PlaceOwnershipStatus;
import kr.omong.dulpick.global.time.ServiceTime;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MemberPlaceResponse(
        @Schema(
                description = "별칭과 저장 시각의 기준 회원 ID입니다. MINE/TOGETHER는 현재 회원, PARTNER는 상대방입니다.",
                example = "123",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long memberId,
        @Schema(
                description = "모든 콘텐츠와 회원 저장 기록이 공유하는 공용 장소 ID입니다. 회원별 저장 기록 ID가 아닙니다.",
                example = "101",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long placeId,
        @Schema(
                description = "Kakao 장소 고유 ID. 검색·상세 API 연계에 사용합니다.",
                example = "18699959",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String kakaoPlaceId,
        @Schema(description = "Kakao에서 확인한 장소명", example = "서울숲 카페", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(description = "Kakao 지번 주소", example = "서울특별시 성동구 성수동1가 685-700", requiredMode = Schema.RequiredMode.REQUIRED)
        String address,
        @Schema(description = "Kakao 도로명 주소. 제공되지 않으면 null입니다.", example = "서울특별시 성동구 서울숲2길 10", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String roadAddress,
        @Schema(description = "WGS84 기준 위도. Kakao가 제공하지 않으면 null입니다.", example = "37.5446", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal latitude,
        @Schema(description = "WGS84 기준 경도. Kakao가 제공하지 않으면 null입니다.", example = "127.0557", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        BigDecimal longitude,
        @Schema(description = "Kakao가 제공한 원본 카테고리 경로. 원본 값을 그대로 보존하며 없으면 null입니다.", example = "음식점 > 카페", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String category,
        @Schema(
                description = "둘픽 화면에서 사용하는 장소 분류입니다.",
                allowableValues = {"맛집", "카페", "놀거리", "쇼핑", "생활 편의", "관광", "숙박"},
                example = "카페",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String categoryName,
        @Schema(
                description = "현재 회원과 연결된 상대방의 저장 관계입니다. MINE은 나만 저장, PARTNER는 상대방만 저장, TOGETHER는 커플 중 한 명 이상이 저장한 장소를 의미합니다.",
                allowableValues = {"MINE", "PARTNER", "TOGETHER"},
                example = "MINE",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        PlaceOwnershipStatus ownershipStatus,
        @Schema(
                description = "회원이 지정한 장소 별칭. 저장 시 별칭을 생략하면 null입니다.",
                example = "주말 데이트 카페",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String alias,
        @Schema(description = "회원이 저장한 시각", example = "2026-08-16T14:30:00", format = "date-time", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime savedAt,
        @Schema(description = "대표 장소 이미지 URL. 이미지가 없으면 null입니다.", example = "https://example.com/place.jpg", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String thumbnailUrl,
        @ArraySchema(schema = @Schema(example = "https://example.com/place-detail.jpg"))
        @Schema(description = "대표 이미지를 제외한 장소 이미지 URL 목록입니다. 이미지가 없으면 빈 배열입니다.", example = "[\"https://example.com/place-detail.jpg\"]", requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> imageUrls
) {

    public static MemberPlaceResponse from(MemberPlaceView view) {
        return new MemberPlaceResponse(
                view.memberId(),
                view.placeId(),
                view.kakaoPlaceId(),
                view.name(),
                view.address(),
                view.roadAddress(),
                view.latitude(),
                view.longitude(),
                view.category(),
                view.categoryName(),
                view.ownershipStatus(),
                view.alias(),
                ServiceTime.toLocalDateTime(view.savedAt()),
                view.thumbnailUrl(),
                view.imageUrls()
        );
    }
}
