package kr.omong.dulpick.domain.auth.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;

public record NonceIssueRequest(
        @Schema(
                description = "로그인할 소셜 제공자. KAKAO는 카카오, GOOGLE은 구글, APPLE은 애플을 의미합니다.",
                allowableValues = {"KAKAO", "GOOGLE", "APPLE"},
                example = "KAKAO"
        )
        @NotNull SocialProvider provider
) {
}
