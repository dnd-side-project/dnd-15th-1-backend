package kr.omong.dulpick.domain.auth.infrastructure.apple;

public interface AppleTokenClient {

    AppleTokenResponse exchange(String authorizationCode, String clientId);

    void revoke(String refreshToken, String clientId);
}
