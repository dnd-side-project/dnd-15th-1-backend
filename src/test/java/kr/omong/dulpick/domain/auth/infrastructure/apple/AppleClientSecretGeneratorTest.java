package kr.omong.dulpick.domain.auth.infrastructure.apple;

import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class AppleClientSecretGeneratorTest {

    @Test
    void generatesValidEs256ClientSecret() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(256);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + privateKey
                + "\n-----END PRIVATE KEY-----";
        AppleTokenProperties properties = new AppleTokenProperties(
                "TEAM_ID",
                "KEY_ID",
                "com.example.dulpick",
                Base64.getEncoder().encodeToString(pem.getBytes()),
                "",
                "https://appleid.apple.com/auth/token",
                "https://appleid.apple.com/auth/revoke",
                Duration.ofMinutes(5)
        );

        String clientSecret = new AppleClientSecretGenerator(properties, Clock.systemUTC())
                .generate();
        SignedJWT signedJwt = SignedJWT.parse(clientSecret);

        assertThat(signedJwt.verify(new ECDSAVerifier((ECPublicKey) keyPair.getPublic()))).isTrue();
        assertThat(signedJwt.getHeader().getKeyID()).isEqualTo("KEY_ID");
        assertThat(signedJwt.getJWTClaimsSet().getIssuer()).isEqualTo("TEAM_ID");
        assertThat(signedJwt.getJWTClaimsSet().getSubject()).isEqualTo("com.example.dulpick");
    }
}
