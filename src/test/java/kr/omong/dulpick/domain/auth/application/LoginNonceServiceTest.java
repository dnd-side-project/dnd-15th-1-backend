package kr.omong.dulpick.domain.auth.application;

import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.global.security.Sha256;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class LoginNonceServiceTest {

    @Autowired
    private LoginNonceService loginNonceService;

    @Test
    void consumesGoogleNonceOnlyOnce() {
        IssuedNonce issuedNonce = loginNonceService.issue(SocialProvider.GOOGLE);

        loginNonceService.consume(
                SocialProvider.GOOGLE,
                issuedNonce.nonce(),
                issuedNonce.nonce()
        );

        assertThatThrownBy(() -> loginNonceService.consume(
                SocialProvider.GOOGLE,
                issuedNonce.nonce(),
                issuedNonce.nonce()
        )).isInstanceOf(InvalidLoginNonceException.class);
    }

    @Test
    void consumesAppleNonceOnlyOnce() {
        IssuedNonce issuedNonce = loginNonceService.issue(SocialProvider.APPLE);
        String tokenNonce = Sha256.hex(issuedNonce.nonce());

        loginNonceService.consume(SocialProvider.APPLE, issuedNonce.nonce(), tokenNonce);

        assertThatThrownBy(() -> loginNonceService.consume(
                SocialProvider.APPLE,
                issuedNonce.nonce(),
                tokenNonce
        )).isInstanceOf(InvalidLoginNonceException.class);
    }

    @Test
    void rejectsMismatchedKakaoNonce() {
        IssuedNonce issuedNonce = loginNonceService.issue(SocialProvider.KAKAO);

        assertThatThrownBy(() -> loginNonceService.consume(
                SocialProvider.KAKAO,
                issuedNonce.nonce(),
                "different-nonce"
        )).isInstanceOf(InvalidLoginNonceException.class);
    }
}
