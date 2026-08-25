package kr.omong.dulpick.domain.place.presentation.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateContentAdminRequest(
        @Size(max = 4_000) String title,
        @Size(max = 100_000) String content
) {
}
