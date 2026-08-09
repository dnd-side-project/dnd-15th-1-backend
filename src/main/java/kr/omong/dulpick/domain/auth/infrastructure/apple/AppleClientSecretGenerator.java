package kr.omong.dulpick.domain.auth.infrastructure.apple;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

public class AppleClientSecretGenerator {

    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    private final AppleTokenProperties properties;
    private final Clock clock;
    private volatile ECPrivateKey privateKey;

    public AppleClientSecretGenerator(AppleTokenProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public String generate(String clientId) {
        validateProperties(clientId);
        Instant issuedAt = clock.instant();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(properties.teamId())
                .subject(clientId)
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
        ECPrivateKey cachedKey = privateKey;
        if (cachedKey != null) {
            return cachedKey;
        }
        synchronized (this) {
            if (privateKey == null) {
                privateKey = loadPrivateKey();
            }
            return privateKey;
        }
    }

    private ECPrivateKey loadPrivateKey() {
        try {
            String pem = Files.readString(Path.of(properties.privateKeyPath()));
            String encodedKey = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
            PrivateKey privateKey = KeyFactory.getInstance("EC")
                    .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
            return (ECPrivateKey) privateKey;
        } catch (IOException exception) {
            throw new AppleAuthorizationException("Apple private key could not be loaded", exception);
        } catch (ClassCastException
                 | IllegalArgumentException
                 | NoSuchAlgorithmException
                 | InvalidKeySpecException exception) {
            throw new AppleAuthorizationException("Apple private key is invalid", exception);
        }
    }

    private void validateProperties(String clientId) {
        if (isBlank(properties.teamId())
                || isBlank(properties.keyId())
                || properties.clientIds() == null
                || properties.clientIds().isEmpty()
                || isBlank(properties.privateKeyPath())
                || properties.clientSecretTtl() == null
                || properties.clientSecretTtl().isNegative()
                || properties.clientSecretTtl().isZero()) {
            throw new AppleAuthorizationException("Apple token configuration is incomplete");
        }
        if (isBlank(clientId) || !properties.clientIds().contains(clientId)) {
            throw new AppleAuthorizationException("Apple client ID is not allowed");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
