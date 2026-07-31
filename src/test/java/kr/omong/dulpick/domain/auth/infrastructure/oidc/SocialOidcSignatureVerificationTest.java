package kr.omong.dulpick.domain.auth.infrastructure.oidc;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SocialOidcSignatureVerificationTest {

    private static final String APPLE_ISSUER = "https://appleid.apple.com";
    private static final String PROD_CLIENT_ID = "com.dulpick.app";
    private static final String DEV_CLIENT_ID = "com.dulpick.dev";
    private static final String GOOGLE_ISSUER = "https://accounts.google.com";
    private static final String GOOGLE_CLIENT_ID = "google-server-client-id";
    private static final String KAKAO_ISSUER = "https://kauth.kakao.com";
    private static final String KAKAO_CLIENT_ID = "kakao-native-app-key";

    private final AtomicReference<JWKSet> currentJwkSet = new AtomicReference<>();
    private final AtomicInteger jwksRequestCount = new AtomicInteger();

    private HttpServer jwksServer;
    private SocialIdentityVerifier verifier;
    private SocialIdentityVerifier googleVerifier;
    private SocialIdentityVerifier kakaoVerifier;

    @BeforeEach
    void setUp() throws IOException {
        jwksServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        jwksServer.createContext("/auth/keys", exchange -> {
            jwksRequestCount.incrementAndGet();
            byte[] body = currentJwkSet.get()
                    .toString()
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set(
                    "Content-Type",
                    "application/json"
            );
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        jwksServer.start();
        createVerifiers();
    }

    @AfterEach
    void tearDown() {
        jwksServer.stop(0);
    }

    @Test
    void acceptsProdAudience() throws Exception {
        RSAKey signingKey = signingKey("prod-key");
        publish(signingKey);

        SocialIdentity identity = verifier.verify(token(
                signingKey,
                APPLE_ISSUER,
                PROD_CLIENT_ID,
                "apple-subject",
                "hashed-nonce",
                Instant.now().plusSeconds(300),
                JWSAlgorithm.RS256
        ));

        assertThat(identity.providerSubject()).isEqualTo("apple-subject");
        assertThat(identity.audience()).isEqualTo(PROD_CLIENT_ID);
        assertThat(identity.tokenNonce()).isEqualTo("hashed-nonce");
    }

    @Test
    void acceptsDevAudience() throws Exception {
        RSAKey signingKey = signingKey("dev-key");
        publish(signingKey);

        SocialIdentity identity = verifier.verify(token(
                signingKey,
                APPLE_ISSUER,
                DEV_CLIENT_ID,
                "apple-subject",
                "hashed-nonce",
                Instant.now().plusSeconds(300),
                JWSAlgorithm.RS256
        ));

        assertThat(identity.audience()).isEqualTo(DEV_CLIENT_ID);
    }

    @Test
    void acceptsGoogleTokenWithServerClientAudienceAndRawNonce() throws Exception {
        RSAKey signingKey = signingKey("google-key");
        publish(signingKey);

        SocialIdentity identity = googleVerifier.verify(token(
                signingKey,
                GOOGLE_ISSUER,
                GOOGLE_CLIENT_ID,
                "google-subject",
                "raw-nonce",
                Instant.now().plusSeconds(300),
                JWSAlgorithm.RS256
        ));

        assertThat(identity.providerSubject()).isEqualTo("google-subject");
        assertThat(identity.audience()).isEqualTo(GOOGLE_CLIENT_ID);
        assertThat(identity.tokenNonce()).isEqualTo("raw-nonce");
        assertThat(identity.email()).isEqualTo("member@example.com");
    }

    @Test
    void acceptsKakaoTokenWithNativeAppAudienceAndRawNonce() throws Exception {
        RSAKey signingKey = signingKey("kakao-key");
        publish(signingKey);

        SocialIdentity identity = kakaoVerifier.verify(token(
                signingKey,
                KAKAO_ISSUER,
                KAKAO_CLIENT_ID,
                "kakao-subject",
                "raw-nonce",
                Instant.now().plusSeconds(300),
                JWSAlgorithm.RS256
        ));

        assertThat(identity.providerSubject()).isEqualTo("kakao-subject");
        assertThat(identity.audience()).isEqualTo(KAKAO_CLIENT_ID);
        assertThat(identity.tokenNonce()).isEqualTo("raw-nonce");
        assertThat(identity.email()).isEqualTo("member@example.com");
    }

    @Test
    void rejectsUnexpectedAudience() throws Exception {
        RSAKey signingKey = signingKey("audience-key");
        publish(signingKey);

        assertVerificationFails(token(
                signingKey,
                APPLE_ISSUER,
                "attacker-client-id",
                "apple-subject",
                "hashed-nonce",
                Instant.now().plusSeconds(300),
                JWSAlgorithm.RS256
        ));
    }

    @Test
    void rejectsUnexpectedIssuer() throws Exception {
        RSAKey signingKey = signingKey("issuer-key");
        publish(signingKey);

        assertVerificationFails(token(
                signingKey,
                "https://attacker.example",
                PROD_CLIENT_ID,
                "apple-subject",
                "hashed-nonce",
                Instant.now().plusSeconds(300),
                JWSAlgorithm.RS256
        ));
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        RSAKey signingKey = signingKey("expired-key");
        publish(signingKey);

        assertVerificationFails(token(
                signingKey,
                APPLE_ISSUER,
                PROD_CLIENT_ID,
                "apple-subject",
                "hashed-nonce",
                Instant.now().minusSeconds(120),
                JWSAlgorithm.RS256
        ));
    }

    @Test
    void rejectsTokenWithoutExpiration() throws Exception {
        RSAKey signingKey = signingKey("missing-expiration-key");
        publish(signingKey);

        assertVerificationFails(token(
                signingKey,
                APPLE_ISSUER,
                PROD_CLIENT_ID,
                "apple-subject",
                "hashed-nonce",
                null,
                JWSAlgorithm.RS256
        ));
    }

    @Test
    void rejectsTokenSignedWithUnknownPrivateKey() throws Exception {
        RSAKey publishedKey = signingKey("published-key");
        RSAKey attackerKey = signingKey("published-key");
        publish(publishedKey);

        assertVerificationFails(token(
                attackerKey,
                APPLE_ISSUER,
                PROD_CLIENT_ID,
                "apple-subject",
                "hashed-nonce",
                Instant.now().plusSeconds(300),
                JWSAlgorithm.RS256
        ));
    }

    @Test
    void rejectsMissingSubject() throws Exception {
        RSAKey signingKey = signingKey("subject-key");
        publish(signingKey);

        assertVerificationFails(token(
                signingKey,
                APPLE_ISSUER,
                PROD_CLIENT_ID,
                null,
                "hashed-nonce",
                Instant.now().plusSeconds(300),
                JWSAlgorithm.RS256
        ));
    }

    @Test
    void selectsMatchingKidFromMultipleAppleKeys() throws Exception {
        RSAKey firstKey = signingKey("first-key");
        RSAKey selectedKey = signingKey("selected-key");
        publish(firstKey, selectedKey);

        SocialIdentity identity = verifier.verify(token(
                selectedKey,
                APPLE_ISSUER,
                PROD_CLIENT_ID,
                "apple-subject",
                "hashed-nonce",
                Instant.now().plusSeconds(300),
                JWSAlgorithm.RS256
        ));

        assertThat(identity.providerSubject()).isEqualTo("apple-subject");
        assertThat(jwksRequestCount).hasValue(1);
    }

    @Test
    void refreshesJwksOnceWhenKidIsUnknown() throws Exception {
        RSAKey previousKey = signingKey("previous-key");
        RSAKey rotatedKey = signingKey("rotated-key");
        publish(previousKey);
        verifier.verify(token(
                previousKey,
                APPLE_ISSUER,
                PROD_CLIENT_ID,
                "apple-subject",
                "hashed-nonce",
                Instant.now().plusSeconds(300),
                JWSAlgorithm.RS256
        ));
        publish(rotatedKey);

        SocialIdentity identity = verifier.verify(token(
                rotatedKey,
                APPLE_ISSUER,
                PROD_CLIENT_ID,
                "apple-subject",
                "hashed-nonce",
                Instant.now().plusSeconds(300),
                JWSAlgorithm.RS256
        ));

        assertThat(identity.providerSubject()).isEqualTo("apple-subject");
        assertThat(jwksRequestCount.get()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void rejectsUnconfiguredSignatureAlgorithm() throws Exception {
        RSAKey signingKey = signingKey("algorithm-key");
        publish(signingKey);

        assertVerificationFails(token(
                signingKey,
                APPLE_ISSUER,
                PROD_CLIENT_ID,
                "apple-subject",
                "hashed-nonce",
                Instant.now().plusSeconds(300),
                JWSAlgorithm.PS256
        ));
    }

    @Test
    void convertsJwksLookupFailureToSocialTokenFailure() throws Exception {
        RSAKey signingKey = signingKey("network-key");
        publish(signingKey);
        jwksServer.stop(0);

        assertVerificationFails(token(
                signingKey,
                APPLE_ISSUER,
                PROD_CLIENT_ID,
                "apple-subject",
                "hashed-nonce",
                Instant.now().plusSeconds(300),
                JWSAlgorithm.RS256
        ));
    }

    private void createVerifiers() {
        String jwkSetUri = "http://127.0.0.1:"
                + jwksServer.getAddress().getPort()
                + "/auth/keys";
        SocialProviderProperties.Provider apple =
                new SocialProviderProperties.Provider(
                        Set.of(APPLE_ISSUER),
                        jwkSetUri,
                        Set.of(PROD_CLIENT_ID, DEV_CLIENT_ID)
                );
        SocialProviderProperties.Provider google =
                new SocialProviderProperties.Provider(
                        Set.of(GOOGLE_ISSUER),
                        jwkSetUri,
                        Set.of(GOOGLE_CLIENT_ID)
                );
        SocialProviderProperties.Provider kakao =
                new SocialProviderProperties.Provider(
                        Set.of(KAKAO_ISSUER),
                        jwkSetUri,
                        Set.of(KAKAO_CLIENT_ID)
                );
        SocialProviderProperties properties =
                new SocialProviderProperties(google, kakao, apple);
        SocialOidcConfig config = new SocialOidcConfig();
        verifier = config.appleSocialIdentityVerifier(properties);
        googleVerifier = config.googleSocialIdentityVerifier(properties);
        kakaoVerifier = config.kakaoSocialIdentityVerifier(properties);
    }

    private RSAKey signingKey(String keyId) throws Exception {
        return new RSAKeyGenerator(2048)
                .keyID(keyId)
                .generate();
    }

    private void publish(RSAKey... keys) {
        currentJwkSet.set(new JWKSet(
                List.of(keys).stream()
                        .map(key -> (JWK) key.toPublicJWK())
                        .toList()
        ));
    }

    private String token(
            RSAKey signingKey,
            String issuer,
            String audience,
            String subject,
            String nonce,
            Instant expiresAt,
            JWSAlgorithm algorithm
    ) throws Exception {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .issueTime(Date.from(Instant.now().minusSeconds(10)))
                .claim("nonce", nonce)
                .claim("email", "member@example.com")
                .claim("email_verified", true);
        if (expiresAt != null) {
            claims.expirationTime(Date.from(expiresAt));
        }
        if (subject != null) {
            claims.subject(subject);
        }
        SignedJWT signedJwt = new SignedJWT(
                new JWSHeader.Builder(algorithm)
                        .type(JOSEObjectType.JWT)
                        .keyID(signingKey.getKeyID())
                        .build(),
                claims.build()
        );
        signedJwt.sign(new RSASSASigner(signingKey));
        return signedJwt.serialize();
    }

    private void assertVerificationFails(String token) {
        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(InvalidSocialTokenException.class)
                .hasMessageNotContaining(token);
    }
}
