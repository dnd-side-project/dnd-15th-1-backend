package kr.omong.dulpick.domain.place.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(description = "운영자 장소 후보 제외 요청")
public record ReviewPlaceCandidateRequest(
        @NotNull
        @Schema(example = "2026-08-24T10:00:05Z", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant expectedUpdatedAt
) {
}
