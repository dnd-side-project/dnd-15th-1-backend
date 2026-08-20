package kr.omong.dulpick.domain.place.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UpdatePlaceAliasRequest(
        @Size(max = 100)
        @Schema(
                description = "회원이 지정한 장소 별칭입니다. null이거나 공백이면 별칭을 제거합니다.",
                example = "주말 데이트 카페",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String alias
) {
}
