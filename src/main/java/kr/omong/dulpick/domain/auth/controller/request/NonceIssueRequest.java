package kr.omong.dulpick.domain.auth.controller.request;

import jakarta.validation.constraints.NotNull;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;

public record NonceIssueRequest(
        @NotNull SocialProvider provider
) {
}
