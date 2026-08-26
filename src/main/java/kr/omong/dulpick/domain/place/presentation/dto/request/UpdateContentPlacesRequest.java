package kr.omong.dulpick.domain.place.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;

import java.util.List;
import java.time.Instant;

public record UpdateContentPlacesRequest(
        @NotNull @Size(max = 50) @ArraySchema(schema = @Schema(example = "101"), arraySchema = @Schema(example = "[101, 102]")) List<@NotNull Long> placeIds,
        @NotNull @Schema(example = "2026-08-24T10:00:05Z") Instant expectedUpdatedAt
) {
}
