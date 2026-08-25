package kr.omong.dulpick.domain.place.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;

import java.time.Instant;
import java.util.List;

public record ReorderPlaceImagesRequest(
        @NotNull @Size(max = 50) @ArraySchema(schema = @Schema(example = "501"), arraySchema = @Schema(example = "[501, 502]")) List<@NotNull Long> imageIds,
        @NotNull @Schema(example = "2026-08-24T10:00:05Z") Instant expectedUpdatedAt
) {
}
