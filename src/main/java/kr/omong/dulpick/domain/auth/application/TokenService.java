package kr.omong.dulpick.domain.auth.application;

import kr.omong.dulpick.domain.auth.domain.RefreshToken;
import kr.omong.dulpick.domain.auth.domain.RefreshTokenRepository;
import kr.omong.dulpick.domain.member.application.MemberNotActiveException;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.global.security.JwtProperties;
import kr.omong.dulpick.global.security.Sha256;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
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
        String refreshToken = generateRefreshToken();
        saveRefreshToken(member, refreshToken);
        return createIssuedTokens(member, refreshToken);
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public IssuedTokens rotate(String rawRefreshToken) {
        String currentHash = Sha256.hex(rawRefreshToken);
        RefreshToken currentToken = refreshTokenRepository
                .findForUpdateByTokenHash(currentHash)
                .orElseThrow(InvalidRefreshTokenException::new);
        rejectRotatedTokenReplay(currentToken);
        validateRefreshToken(currentToken);

        String newRefreshToken = generateRefreshToken();
        String newTokenHash = Sha256.hex(newRefreshToken);
        currentToken.rotate(newTokenHash);
        saveRefreshToken(currentToken.getMember(), newRefreshToken);
        return createIssuedTokens(currentToken.getMember(), newRefreshToken);
    }

    @Transactional
    public void revoke(String rawRefreshToken, Long memberId) {
        refreshTokenRepository
                .findForUpdateByTokenHash(Sha256.hex(rawRefreshToken))
                .filter(token -> token.getMember().getId().equals(memberId))
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
                .claim("tokenVersion", member.getTokenVersion())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private void saveRefreshToken(Member member, String rawRefreshToken) {
        RefreshToken refreshToken = RefreshToken.create(
                member,
                Sha256.hex(rawRefreshToken),
                clock.instant().plus(properties.refreshTokenTtl())
        );
        refreshTokenRepository.save(refreshToken);
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void validateRefreshToken(RefreshToken refreshToken) {
        if (refreshToken.isRevoked()) {
            throw new InvalidRefreshTokenException();
        }
        if (refreshToken.isExpired(clock.instant())) {
            throw new ExpiredRefreshTokenException();
        }
        if (!refreshToken.getMember().isActive()) {
            throw new MemberNotActiveException();
        }
    }

    private void rejectRotatedTokenReplay(RefreshToken refreshToken) {
        if (!refreshToken.wasRotated()) {
            return;
        }
        refreshTokenRepository.revokeAllByMemberId(
                refreshToken.getMember().getId(),
                clock.instant()
        );
        throw new InvalidRefreshTokenException();
    }

}
