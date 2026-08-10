package kr.omong.dulpick.domain.place.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ManualPlaceSaveRequest(
        @NotBlank
        @Size(min = 1, max = 80)
        @Schema(description = "Kakao 검색 결과의 장소 ID")
        String kakaoPlaceId,
        @NotBlank
        @Size(min = 1, max = 200)
        @Schema(description = "Kakao 검색에 사용한 검색어")
        String query,
        @Size(max = 100)
        @Schema(description = "연결된 상대방에게 보일 별칭")
        String alias,
        @Size(max = 1_000)
        @Schema(description = "장소 메모")
        String memo
) {
}
