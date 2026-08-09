package kr.omong.dulpick.domain.auth.infrastructure.apple;

import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppleClientSecretGeneratorTest {

    private static final Instant NOW = Instant.parse("2026-07-31T12:00:00Z");
    private static final String PROD_CLIENT_ID = "com.dulpick.app";
    private static final String DEV_CLIENT_ID = "com.dulpick.dev";

    @TempDir
    private Path tempDirectory;

    @Test
    void generatesValidEs256ClientSecretForProdAndDevClientIds() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(256);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + privateKey
                + "\n-----END PRIVATE KEY-----";
        Path privateKeyPath = tempDirectory.resolve("AuthKey_TEST.p8");
        Files.writeString(privateKeyPath, pem);
        AppleTokenProperties properties = new AppleTokenProperties(
                "TEAM_ID",
                "KEY_ID",
                Set.of(PROD_CLIENT_ID, DEV_CLIENT_ID),
                privateKeyPath.toString(),
                "",
                "https://appleid.apple.com/auth/token",
                "https://appleid.apple.com/auth/revoke",
                Duration.ofMinutes(5)
        );
        AppleClientSecretGenerator generator = new AppleClientSecretGenerator(
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        assertClientSecret(generator.generate(PROD_CLIENT_ID), PROD_CLIENT_ID, keyPair);
        Files.delete(privateKeyPath);
        assertClientSecret(generator.generate(DEV_CLIENT_ID), DEV_CLIENT_ID, keyPair);
    }

    @Test
    void rejectsUnconfiguredClientIdWithoutLoadingPrivateKey() {
        AppleTokenProperties properties = new AppleTokenProperties(
                "TEAM_ID",
                "KEY_ID",
                Set.of(PROD_CLIENT_ID, DEV_CLIENT_ID),
                tempDirectory.resolve("missing.p8").toString(),
                "",
                "https://appleid.apple.com/auth/token",
                "https://appleid.apple.com/auth/revoke",
                Duration.ofMinutes(5)
        );
        AppleClientSecretGenerator generator = new AppleClientSecretGenerator(
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        assertThatThrownBy(() -> generator.generate("attacker-client-id"))
                .isInstanceOf(AppleAuthorizationException.class)
                .hasMessageNotContaining("attacker-client-id");
    }

    private void assertClientSecret(
            String clientSecret,
            String clientId,
            KeyPair keyPair
    ) throws Exception {
        SignedJWT signedJwt = SignedJWT.parse(clientSecret);
        assertThat(signedJwt.verify(new ECDSAVerifier((ECPublicKey) keyPair.getPublic()))).isTrue();
        assertThat(signedJwt.getHeader().getAlgorithm().getName()).isEqualTo("ES256");
        assertThat(signedJwt.getHeader().getKeyID()).isEqualTo("KEY_ID");
        assertThat(signedJwt.getJWTClaimsSet().getIssuer()).isEqualTo("TEAM_ID");
        assertThat(signedJwt.getJWTClaimsSet().getSubject()).isEqualTo(clientId);
        assertThat(signedJwt.getJWTClaimsSet().getAudience())
                .containsExactly("https://appleid.apple.com");
        assertThat(signedJwt.getJWTClaimsSet().getIssueTime()).isEqualTo(NOW);
        assertThat(signedJwt.getJWTClaimsSet().getExpirationTime())
                .isEqualTo(NOW.plus(Duration.ofMinutes(5)));
    }
}
