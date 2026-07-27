package kr.omong.dulpick.domain.auth.infrastructure.apple;

public interface AppleTokenClient {

    AppleTokenResponse exchange(String authorizationCode);

    void revoke(String refreshToken);
}
