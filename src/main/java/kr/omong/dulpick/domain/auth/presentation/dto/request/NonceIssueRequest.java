package kr.omong.dulpick.domain.auth.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;

public record NonceIssueRequest(
        @Schema(
                description = "로그인할 소셜 제공자",
                allowableValues = {"KAKAO", "GOOGLE", "APPLE"},
                example = "KAKAO"
        )
        @NotNull SocialProvider provider
) {
}
