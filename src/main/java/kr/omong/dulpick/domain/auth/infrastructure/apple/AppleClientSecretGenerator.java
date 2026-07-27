package kr.omong.dulpick.domain.auth.infrastructure.apple;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

public class AppleClientSecretGenerator {

    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    private final AppleTokenProperties properties;
    private final Clock clock;

    public AppleClientSecretGenerator(AppleTokenProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public String generate() {
        validateProperties();
        Instant issuedAt = clock.instant();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(properties.teamId())
                .subject(properties.clientId())
                .audience(APPLE_ISSUER)
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(issuedAt.plus(properties.clientSecretTtl())))
                .build();
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .keyID(properties.keyId())
                .build();
        SignedJWT signedJwt = new SignedJWT(header, claims);
        sign(signedJwt);
        return signedJwt.serialize();
    }

    private void sign(SignedJWT signedJwt) {
        try {
            signedJwt.sign(new ECDSASigner(readPrivateKey()));
        } catch (JOSEException exception) {
            throw new AppleAuthorizationException("Failed to sign Apple client secret", exception);
        }
    }

    private ECPrivateKey readPrivateKey() {
        try {
            byte[] pemBytes = Base64.getDecoder().decode(properties.privateKeyBase64());
            String pem = new String(pemBytes, StandardCharsets.UTF_8);
            String encodedKey = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
            PrivateKey privateKey = KeyFactory.getInstance("EC")
                    .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
            return (ECPrivateKey) privateKey;
        } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException exception) {
            throw new AppleAuthorizationException("Apple private key is invalid", exception);
        }
    }

    private void validateProperties() {
        if (isBlank(properties.teamId())
                || isBlank(properties.keyId())
                || isBlank(properties.clientId())
                || isBlank(properties.privateKeyBase64())) {
            throw new AppleAuthorizationException("Apple token configuration is incomplete");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
