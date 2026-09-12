package kr.omong.dulpick.domain.place.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(description = "운영자 장소 추출 최종 확정 요청")
public record CompletePlaceImportRequest(
        @NotNull
        @Schema(example = "2026-08-24T10:00:05Z", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant expectedUpdatedAt
) {
}
