package kr.omong.dulpick.domain.place.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ManualPlaceSaveRequest(
        @NotBlank
        @Size(min = 1, max = 80)
        @Schema(
                description = "필수 입력. Kakao 검색 결과의 장소 ID입니다.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "18699959"
        )
        String kakaoPlaceId,
        @NotBlank
        @Size(min = 1, max = 200)
        @Schema(
                description = "필수 입력. Kakao 장소를 확인하기 위해 사용한 검색어입니다.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "성수동 카페"
        )
        String query,
        @Size(max = 100)
        @Schema(
                description = "선택 입력. 연결된 상대방에게 보일 장소 별칭입니다. 생략하거나 null이면 별칭 없이 저장합니다.",
                example = "주말 데이트 카페",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String alias
) {
}
