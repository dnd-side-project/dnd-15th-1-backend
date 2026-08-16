package kr.omong.dulpick.domain.date.presentation.dto.response;

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
        List<DateCoursePlaceCandidateResponse> places,
        List<String> availableRegions,
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
            DulpickPlaceCategory code,
            String name
    ) {

        private static DateCourseCategoryOptionResponse from(DateCourseCategoryOptionView view) {
            return new DateCourseCategoryOptionResponse(view.code(), view.name());
        }
    }

    public record DateCoursePlaceCandidateResponse(
            Long placeId,
            String name,
            String address,
            String roadAddress,
            @Schema(description = "지역 카테고리 필터에 사용할 대표 지역명")
            String region,
            BigDecimal latitude,
            BigDecimal longitude,
            String category,
            String categoryName,
            PlaceOwnershipStatus ownershipStatus,
            String alias,
            LocalDateTime savedAt,
            String thumbnailUrl,
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
