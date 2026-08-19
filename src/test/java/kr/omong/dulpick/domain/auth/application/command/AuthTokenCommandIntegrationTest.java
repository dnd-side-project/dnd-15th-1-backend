package kr.omong.dulpick.domain.auth.application.command;

import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.exception.ExpiredRefreshTokenException;
import kr.omong.dulpick.domain.auth.application.exception.InvalidRefreshTokenException;
import kr.omong.dulpick.domain.auth.application.support.TokenService;
import kr.omong.dulpick.domain.auth.domain.RefreshToken;
import kr.omong.dulpick.domain.auth.domain.RefreshTokenRepository;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.global.security.config.JwtProperties;
import kr.omong.dulpick.global.security.crypto.Sha256;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = "auth.jwt.refresh-token-replay-grace=0s")
@Transactional
class AuthTokenCommandIntegrationTest {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AuthCommandService authCommandService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void issuesAccessAndRefreshTokens() {
        Member member = memberRepository.save(Member.create(Instant.EPOCH));

        IssuedTokens tokens = tokenService.issue(member);
        Jwt jwt = jwtDecoder.decode(tokens.accessToken());

        assertThat(jwt.getSubject()).isEqualTo(member.getId().toString());
        assertThat(jwt.getClaimAsString("type")).isEqualTo("access");
        Number tokenVersion = jwt.getClaim("tokenVersion");
        assertThat(tokenVersion.longValue()).isZero();
        assertThat(tokens.refreshToken()).isNotBlank();
        assertThat(tokens.accessTokenExpiresIn())
                .isEqualTo(jwtProperties.accessTokenTtl().toSeconds());
    }

    @Test
    void revokesTokenFamilyWhenRotatedTokenIsReplayed() {
        Member member = memberRepository.save(Member.create(Instant.EPOCH));
        IssuedTokens issuedTokens = tokenService.issue(member);

        IssuedTokens rotatedTokens = authCommandService.reissue(issuedTokens.refreshToken());
        IssuedTokens otherSessionTokens = tokenService.issue(member);

        assertThat(rotatedTokens.refreshToken()).isNotEqualTo(issuedTokens.refreshToken());
        assertThatThrownBy(() -> authCommandService.reissue(issuedTokens.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> authCommandService.reissue(rotatedTokens.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> authCommandService.reissue(otherSessionTokens.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void revokesOnlyTokenOwnedByAuthenticatedMember() {
        Member owner = memberRepository.save(Member.create(Instant.EPOCH));
        Member otherMember = memberRepository.save(Member.create(Instant.EPOCH));
        IssuedTokens issuedTokens = tokenService.issue(owner);

        authCommandService.logout(issuedTokens.refreshToken(), otherMember.getId());
        IssuedTokens rotatedTokens = authCommandService.reissue(issuedTokens.refreshToken());
        authCommandService.logout(rotatedTokens.refreshToken(), owner.getId());

        assertThatThrownBy(() -> authCommandService.reissue(rotatedTokens.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void revokesReplacementChainWhenLogoutUsesRotatedToken() {
        Member member = memberRepository.save(Member.create(Instant.EPOCH));
        IssuedTokens initialTokens = tokenService.issue(member);
        IssuedTokens rotatedTokens = authCommandService.reissue(initialTokens.refreshToken());
        IssuedTokens latestTokens = authCommandService.reissue(rotatedTokens.refreshToken());
        IssuedTokens otherSessionTokens = tokenService.issue(member);

        authCommandService.logout(initialTokens.refreshToken(), member.getId());

        assertThatThrownBy(() -> authCommandService.reissue(latestTokens.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThat(authCommandService.reissue(otherSessionTokens.refreshToken()))
                .isNotNull();
    }

    @Test
    void distinguishesExpiredRefreshToken() {
        Member member = memberRepository.save(Member.create(Instant.EPOCH));
        String rawRefreshToken = "expired-refresh-token";
        RefreshToken refreshToken = RefreshToken.create(
                member,
                Sha256.hex(rawRefreshToken),
                Instant.EPOCH,
                Instant.EPOCH
        );
        refreshTokenRepository.saveAndFlush(refreshToken);

        assertThatThrownBy(() -> authCommandService.reissue(rawRefreshToken))
                .isInstanceOf(ExpiredRefreshTokenException.class);
    }
}
