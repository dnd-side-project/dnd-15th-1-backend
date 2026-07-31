package kr.omong.dulpick.domain.auth.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(
        @Schema(
                description = "둘픽 로그인 또는 직전 재발급 응답으로 받은 Refresh Token",
                example = "eyJhbGciOiJIUzI1NiJ9..."
        )
        @NotBlank String refreshToken
) {
}
