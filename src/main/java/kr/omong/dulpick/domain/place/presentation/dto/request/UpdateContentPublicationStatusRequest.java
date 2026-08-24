package kr.omong.dulpick.domain.place.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import kr.omong.dulpick.domain.place.domain.ContentPublicationStatus;

public record UpdateContentPublicationStatusRequest(
        @NotNull @Schema(example = "PUBLIC") ContentPublicationStatus publicationStatus
) {
}
