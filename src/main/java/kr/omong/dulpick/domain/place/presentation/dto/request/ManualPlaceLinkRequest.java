package kr.omong.dulpick.domain.place.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

public record ManualPlaceLinkRequest(
        @NotNull Long placeId,
        Long candidateId,
        boolean publish
) {
}
