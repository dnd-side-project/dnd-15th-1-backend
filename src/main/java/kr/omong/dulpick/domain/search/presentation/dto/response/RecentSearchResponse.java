package kr.omong.dulpick.domain.search.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.search.application.RecentSearchView;
import kr.omong.dulpick.domain.search.domain.RecentSearchType;
import kr.omong.dulpick.global.time.ServiceTime;

import java.time.LocalDateTime;

public record RecentSearchResponse(
        @Schema(description = "최근 검색어 ID", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
        Long recentSearchId,
        @Schema(
                description = "검색 도메인",
                allowableValues = {"CONTENT", "PLACE"},
                example = "PLACE",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        RecentSearchType type,
        @Schema(description = "표시할 최근 검색어", example = "성수동 카페", requiredMode = Schema.RequiredMode.REQUIRED)
        String keyword,
        @Schema(
                description = "마지막으로 검색한 시각",
                example = "2026-08-18T07:30:00",
                format = "date-time",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        LocalDateTime searchedAt
) {

    public static RecentSearchResponse from(RecentSearchView view) {
        return new RecentSearchResponse(
                view.recentSearchId(),
                view.type(),
                view.keyword(),
                ServiceTime.toLocalDateTime(view.searchedAt())
        );
    }
}
