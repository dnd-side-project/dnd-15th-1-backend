package kr.omong.dulpick.domain.place.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaceImportRequest(
        @NotBlank
        @Size(max = 2_000)
        @Schema(
                description = "필수 입력. Instagram 게시물 또는 릴스 URL입니다.",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "https://www.instagram.com/reel/example/"
        )
        String sourceUrl
) {
}
