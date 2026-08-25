package kr.omong.dulpick.domain.place.presentation.dto.request;

import jakarta.validation.constraints.Size;

public record UpdatePlaceAdminRequest(
        @Size(max = 255) String name,
        @Size(max = 500) String address,
        @Size(max = 500) String roadAddress,
        @Size(max = 100) String category,
        @Size(max = 3) String categoryGroupCode,
        @Size(max = 50) String phone,
        @Size(max = 1_000) String kakaoPlaceUrl
) {
}
