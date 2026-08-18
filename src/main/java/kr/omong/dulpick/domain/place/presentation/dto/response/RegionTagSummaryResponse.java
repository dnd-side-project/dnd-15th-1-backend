package kr.omong.dulpick.domain.place.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.place.application.RegionTagSummaryView;

public record RegionTagSummaryResponse(
        @Schema(description = "지역 태그 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long regionTagId,
        @Schema(description = "지역 태그명", example = "성수", requiredMode = Schema.RequiredMode.REQUIRED)
        String name
) {

    public static RegionTagSummaryResponse from(RegionTagSummaryView view) {
        return new RegionTagSummaryResponse(view.regionTagId(), view.name());
    }
}
