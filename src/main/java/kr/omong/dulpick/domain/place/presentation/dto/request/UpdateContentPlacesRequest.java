package kr.omong.dulpick.domain.place.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateContentPlacesRequest(
        @NotNull @Size(max = 50) List<@NotNull Long> placeIds
) {
}
