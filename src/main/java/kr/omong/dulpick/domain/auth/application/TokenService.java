package kr.omong.dulpick.domain.auth.application;

import kr.omong.dulpick.domain.auth.domain.RefreshToken;
import kr.omong.dulpick.domain.auth.domain.RefreshTokenRepository;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.global.security.JwtProperties;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class TokenService {

    private static final int REFRESH_TOKEN_BYTES = 32;

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom;
    private final Clock clock;

    public TokenService(
            JwtEncoder jwtEncoder,
            JwtProperties properties,
            RefreshTokenRepository refreshTokenRepository,
            SecureRandom secureRandom,
            Clock clock
    ) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.refreshTokenRepository = refreshTokenRepository;
        this.secureRandom = secureRandom;
        this.clock = clock;
    }

    @Transactional
    public IssuedTokens issue(Member member) {
        String refreshToken = generateRefreshToken();
        saveRefreshToken(member, refreshToken);
        return createIssuedTokens(member, refreshToken);
    }

    @Transactional
    public IssuedTokens rotate(String rawRefreshToken) {
        String currentHash = hash(rawRefreshToken);
        RefreshToken currentToken = refreshTokenRepository.findByTokenHash(currentHash)
                .filter(token -> token.isUsable(clock.instant()))
                .orElseThrow(InvalidRefreshTokenException::new);

        String newRefreshToken = generateRefreshToken();
        String newTokenHash = hash(newRefreshToken);
        currentToken.rotate(newTokenHash);
        saveRefreshToken(currentToken.getMember(), newRefreshToken);
        return createIssuedTokens(currentToken.getMember(), newRefreshToken);
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .ifPresent(RefreshToken::revoke);
    }

    private IssuedTokens createIssuedTokens(Member member, String refreshToken) {
        String accessToken = createAccessToken(member);
        return new IssuedTokens(
                accessToken,
                refreshToken,
                properties.accessTokenTtl().toSeconds()
        );
    }

    private String createAccessToken(Member member) {
        Instant issuedAt = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(member.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(properties.accessTokenTtl()))
                .id(UUID.randomUUID().toString())
                .claim("type", "access")
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private void saveRefreshToken(Member member, String rawRefreshToken) {
        RefreshToken refreshToken = RefreshToken.create(
                member,
                hash(rawRefreshToken),
                clock.instant().plus(properties.refreshTokenTtl())
        );
        refreshTokenRepository.save(refreshToken);
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
