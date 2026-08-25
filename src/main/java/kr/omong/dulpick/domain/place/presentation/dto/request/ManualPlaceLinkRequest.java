package kr.omong.dulpick.domain.place.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record ManualPlaceLinkRequest(
        @NotNull @Schema(example = "101") Long placeId,
        @Schema(example = "3001") Long candidateId,
        @Schema(example = "true")
        boolean publish,
        @NotNull @Schema(example = "2026-08-24T10:00:05Z") Instant expectedUpdatedAt
) {
}
