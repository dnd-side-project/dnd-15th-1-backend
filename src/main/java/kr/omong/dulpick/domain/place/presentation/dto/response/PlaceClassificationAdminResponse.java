package kr.omong.dulpick.domain.place.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.place.application.PlaceClassificationAdminView;
import kr.omong.dulpick.domain.place.domain.ClassificationSource;
import kr.omong.dulpick.domain.place.domain.PlaceActivity;
import kr.omong.dulpick.domain.place.domain.PlaceClassificationStatus;
import kr.omong.dulpick.domain.place.domain.PlaceEnvironment;
import kr.omong.dulpick.domain.place.domain.PlaceFocus;
import kr.omong.dulpick.domain.place.domain.PlaceTime;

public record PlaceClassificationAdminResponse(
        @Schema(description = "공용 장소 ID", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
        Long placeId,
        @Schema(description = "Kakao 장소 ID", example = "18699959", requiredMode = Schema.RequiredMode.REQUIRED)
        String kakaoPlaceId,
        @Schema(description = "장소명", example = "서울숲 카페", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(description = "지번 주소", example = "서울특별시 성동구 성수동1가", requiredMode = Schema.RequiredMode.REQUIRED)
        String address,
        @Schema(description = "도로명 주소", example = "서울특별시 성동구 성수이로", nullable = true)
        String roadAddress,
        @Schema(description = "둘픽 카테고리 표시명", example = "카페", requiredMode = Schema.RequiredMode.REQUIRED)
        String categoryName,
        @Schema(description = "대표 이미지 URL", example = "https://example.com/place.jpg", nullable = true)
        String thumbnailUrl,
        @Schema(
                description = "네 축 분류 상태",
                allowableValues = {"UNCLASSIFIED", "PARTIALLY_CLASSIFIED", "CLASSIFIED"},
                example = "PARTIALLY_CLASSIFIED",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        PlaceClassificationStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        AxisResponse<PlaceEnvironment> environment,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        AxisResponse<PlaceActivity> activity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        AxisResponse<PlaceTime> time,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        AxisResponse<PlaceFocus> focus
) {

    public static PlaceClassificationAdminResponse from(PlaceClassificationAdminView view) {
        return new PlaceClassificationAdminResponse(
                view.placeId(),
                view.kakaoPlaceId(),
                view.name(),
                view.address(),
                view.roadAddress(),
                view.categoryName(),
                view.thumbnailUrl(),
                view.status(),
                AxisResponse.from(view.environment()),
                AxisResponse.from(view.activity()),
                AxisResponse.from(view.time()),
                AxisResponse.from(view.focus())
        );
    }

    public record AxisResponse<T>(
            @Schema(description = "축 값. 미분류면 null", nullable = true)
            T value,
            @Schema(
                    description = "값 출처. 미분류면 null",
                    allowableValues = {"AI", "MANUAL"},
                    example = "MANUAL",
                    nullable = true
            )
            ClassificationSource source
    ) {

        private static <T> AxisResponse<T> from(PlaceClassificationAdminView.AxisView<T> axis) {
            return new AxisResponse<>(axis.value(), axis.source());
        }
    }
}
