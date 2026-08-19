package kr.omong.dulpick.domain.auth.application.support;

import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;
import kr.omong.dulpick.domain.auth.domain.RefreshToken;
import kr.omong.dulpick.domain.auth.domain.RefreshTokenRepository;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.exception.MemberNotActiveException;
import kr.omong.dulpick.global.security.config.JwtProperties;
import kr.omong.dulpick.global.security.crypto.Sha256;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
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
        if (!member.isActive()) {
            throw new MemberNotActiveException();
        }
        Instant issuedAt = clock.instant();
        String refreshToken = generateRefreshToken();
        saveRefreshToken(member, refreshToken, issuedAt);
        return createIssuedTokens(member, refreshToken);
    }

    public IssuedTokens issueRotated(RefreshToken currentToken, String rawRefreshToken) {
        Instant issuedAt = clock.instant();
        String newRefreshToken = generateReplacementRefreshToken(rawRefreshToken);
        String newTokenHash = Sha256.hex(newRefreshToken);
        currentToken.rotate(newTokenHash, issuedAt);
        saveRefreshToken(currentToken.getMember(), newRefreshToken, issuedAt);
        return createIssuedTokens(currentToken.getMember(), newRefreshToken);
    }

    public Optional<IssuedTokens> issueWithinReplayGrace(
            RefreshToken currentToken,
            String rawRefreshToken
    ) {
        Instant now = clock.instant();
        if (!currentToken.isWithinReplayGrace(now, properties.refreshTokenReplayGrace())) {
            return Optional.empty();
        }
        String replacementToken = generateReplacementRefreshToken(rawRefreshToken);
        String replacementHash = Sha256.hex(replacementToken);
        if (!currentToken.matchesReplacementHash(replacementHash)) {
            return Optional.empty();
        }
        return refreshTokenRepository.findByTokenHash(replacementHash)
                .filter(token -> !token.isRevoked())
                .filter(token -> !token.isExpired(now))
                .filter(token -> token.getMember().getId().equals(currentToken.getMember().getId()))
                .map(token -> createIssuedTokens(currentToken.getMember(), replacementToken));
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
                .claim("tokenVersion", member.getTokenVersion())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private void saveRefreshToken(
            Member member,
            String rawRefreshToken,
            Instant issuedAt
    ) {
        RefreshToken refreshToken = RefreshToken.create(
                member,
                Sha256.hex(rawRefreshToken),
                issuedAt.plus(properties.refreshTokenTtl()),
                issuedAt
        );
        refreshTokenRepository.save(refreshToken);
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateReplacementRefreshToken(String rawRefreshToken) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(properties.secretKey());
            mac.update("dulpick-refresh-rotation-v1:".getBytes(StandardCharsets.UTF_8));
            byte[] token = mac.doFinal(rawRefreshToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Refresh token generation failed", exception);
        }
    }

}
