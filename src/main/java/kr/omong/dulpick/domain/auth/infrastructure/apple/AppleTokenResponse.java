package kr.omong.dulpick.domain.auth.infrastructure.apple;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AppleTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("id_token") String idToken
) {
}
