package kr.omong.dulpick.domain.search.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.search.application.RecentSearchView;
import org.springframework.data.domain.Page;

import java.util.List;

public record RecentSearchPageResponse(
        @Schema(description = "현재 페이지의 최근 검색어 목록", example = "[]", requiredMode = Schema.RequiredMode.REQUIRED)
        List<RecentSearchResponse> recentSearches,
        @Schema(description = "0부터 시작하는 현재 페이지 번호", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        int page,
        @Schema(description = "현재 페이지 크기", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
        int size,
        @Schema(description = "해당 검색 도메인의 전체 최근 검색어 수", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
        long totalElements,
        @Schema(description = "전체 페이지 수", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        int totalPages,
        @Schema(description = "다음 페이지 존재 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean hasNext
) {

    public static RecentSearchPageResponse from(Page<RecentSearchView> page) {
        return new RecentSearchPageResponse(
                page.getContent().stream().map(RecentSearchResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
