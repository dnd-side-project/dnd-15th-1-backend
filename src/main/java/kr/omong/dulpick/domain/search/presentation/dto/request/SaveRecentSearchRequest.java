package kr.omong.dulpick.domain.search.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.omong.dulpick.domain.search.domain.RecentSearchType;

public record SaveRecentSearchRequest(
        @NotNull
        @Schema(
                description = "검색어를 구분해서 저장할 검색 도메인",
                allowableValues = {"CONTENT", "PLACE"},
                example = "PLACE",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        RecentSearchType type,
        @NotBlank
        @Size(max = 200)
        @Schema(
                description = "저장할 검색어. 앞뒤 공백과 연속 공백은 정규화됩니다.",
                example = "성수동 카페",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String keyword
) {
}
