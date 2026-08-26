package kr.omong.dulpick.domain.place.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.place.application.PlaceClassificationAdminPage;

import java.util.List;

public record PlaceClassificationAdminPageResponse(
        @ArraySchema(schema = @Schema(implementation = PlaceClassificationAdminResponse.class))
        @Schema(description = "현재 페이지의 장소 분류 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        List<PlaceClassificationAdminResponse> places,
        @Schema(description = "0부터 시작하는 페이지 번호", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        int page,
        @Schema(description = "페이지 크기", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
        int size,
        @Schema(description = "필터에 맞는 장소 수", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
        long totalElements,
        @Schema(description = "전체 페이지 수", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        int totalPages,
        @Schema(description = "다음 페이지 존재 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean hasNext,
        @Schema(description = "검색어 기준 상태별 건수", requiredMode = Schema.RequiredMode.REQUIRED)
        StatusCountsResponse counts
) {

    public static PlaceClassificationAdminPageResponse from(PlaceClassificationAdminPage page) {
        return new PlaceClassificationAdminPageResponse(
                page.places().stream().map(PlaceClassificationAdminResponse::from).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                page.hasNext(),
                StatusCountsResponse.from(page.counts())
        );
    }

    public record StatusCountsResponse(
            @Schema(example = "40", requiredMode = Schema.RequiredMode.REQUIRED)
            long all,
            @Schema(example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
            long unclassified,
            @Schema(example = "8", requiredMode = Schema.RequiredMode.REQUIRED)
            long partiallyClassified,
            @Schema(example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
            long classified
    ) {

        private static StatusCountsResponse from(PlaceClassificationAdminPage.StatusCounts counts) {
            return new StatusCountsResponse(
                    counts.all(),
                    counts.unclassified(),
                    counts.partiallyClassified(),
                    counts.classified()
            );
        }
    }
}
