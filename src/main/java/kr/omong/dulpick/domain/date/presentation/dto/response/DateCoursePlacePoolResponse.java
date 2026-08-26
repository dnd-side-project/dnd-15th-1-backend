package kr.omong.dulpick.domain.date.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.date.application.query.view.DateCourseCategoryOptionView;
import kr.omong.dulpick.domain.date.application.query.view.DateCoursePlaceCandidateView;
import kr.omong.dulpick.domain.date.application.query.view.DateCoursePlacePoolView;
import kr.omong.dulpick.domain.place.domain.DulpickPlaceCategory;
import kr.omong.dulpick.domain.place.domain.PlaceOwnershipStatus;
import kr.omong.dulpick.global.time.ServiceTime;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DateCoursePlacePoolResponse(
        @Schema(example = "[]")
        List<DateCoursePlaceCandidateResponse> places,
        @ArraySchema(schema = @Schema(example = "성동구"))
        @Schema(example = "[]")
        List<String> availableRegions,
        @Schema(example = "[]")
        List<DateCourseCategoryOptionResponse> availableCategories
) {

    public static DateCoursePlacePoolResponse from(DateCoursePlacePoolView view) {
        return new DateCoursePlacePoolResponse(
                view.places().stream().map(DateCoursePlaceCandidateResponse::from).toList(),
                view.availableRegions(),
                view.availableCategories().stream().map(DateCourseCategoryOptionResponse::from).toList()
        );
    }

    public record DateCourseCategoryOptionResponse(
            @Schema(example = "CAFE")
            DulpickPlaceCategory code,
            @Schema(example = "카페")
            String name
    ) {

        private static DateCourseCategoryOptionResponse from(DateCourseCategoryOptionView view) {
            return new DateCourseCategoryOptionResponse(view.code(), view.name());
        }
    }

    public record DateCoursePlaceCandidateResponse(
            @Schema(example = "101")
            Long placeId,
            @Schema(example = "서울숲 카페")
            String name,
            @Schema(example = "서울특별시 성동구 성수동1가 685-700")
            String address,
            @Schema(example = "서울특별시 성동구 서울숲2길 10")
            String roadAddress,
            @Schema(description = "지역 카테고리 필터에 사용할 대표 지역명", example = "성동구")
            String region,
            @Schema(example = "37.5446")
            BigDecimal latitude,
            @Schema(example = "127.0557")
            BigDecimal longitude,
            @Schema(example = "음식점 > 카페")
            String category,
            @Schema(example = "카페")
            String categoryName,
            @Schema(example = "MINE")
            PlaceOwnershipStatus ownershipStatus,
            @Schema(example = "주말 데이트 카페")
            String alias,
            @Schema(example = "2026-08-16T14:30:00")
            LocalDateTime savedAt,
            @Schema(example = "https://example.com/place.jpg")
            String thumbnailUrl,
            @ArraySchema(schema = @Schema(example = "https://example.com/place-detail.jpg"))
            @Schema(example = "[]")
            List<String> imageUrls
    ) {

        private static DateCoursePlaceCandidateResponse from(DateCoursePlaceCandidateView view) {
            return new DateCoursePlaceCandidateResponse(
                    view.placeId(),
                    view.name(),
                    view.address(),
                    view.roadAddress(),
                    view.region(),
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
}
