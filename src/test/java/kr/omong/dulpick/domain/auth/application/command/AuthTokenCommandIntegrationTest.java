package kr.omong.dulpick.domain.auth.application.command;

import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.exception.ExpiredRefreshTokenException;
import kr.omong.dulpick.domain.auth.application.exception.InvalidRefreshTokenException;
import kr.omong.dulpick.domain.auth.application.support.TokenService;
import kr.omong.dulpick.domain.auth.domain.RefreshToken;
import kr.omong.dulpick.domain.auth.domain.RefreshTokenRepository;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.global.security.Sha256;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
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

    @Test
    void issuesAccessAndRefreshTokens() {
        Member member = memberRepository.save(Member.create());

        IssuedTokens tokens = tokenService.issue(member);
        Jwt jwt = jwtDecoder.decode(tokens.accessToken());

        assertThat(jwt.getSubject()).isEqualTo(member.getId().toString());
        assertThat(jwt.getClaimAsString("type")).isEqualTo("access");
        Number tokenVersion = jwt.getClaim("tokenVersion");
        assertThat(tokenVersion.longValue()).isZero();
        assertThat(tokens.refreshToken()).isNotBlank();
        assertThat(tokens.accessTokenExpiresIn()).isEqualTo(900);
    }

    @Test
    void revokesTokenFamilyWhenRotatedTokenIsReplayed() {
        Member member = memberRepository.save(Member.create());
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
        Member owner = memberRepository.save(Member.create());
        Member otherMember = memberRepository.save(Member.create());
        IssuedTokens issuedTokens = tokenService.issue(owner);

        authCommandService.logout(issuedTokens.refreshToken(), otherMember.getId());
        IssuedTokens rotatedTokens = authCommandService.reissue(issuedTokens.refreshToken());
        authCommandService.logout(rotatedTokens.refreshToken(), owner.getId());

        assertThatThrownBy(() -> authCommandService.reissue(rotatedTokens.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void distinguishesExpiredRefreshToken() {
        Member member = memberRepository.save(Member.create());
        IssuedTokens tokens = tokenService.issue(member);
        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(Sha256.hex(tokens.refreshToken()))
                .orElseThrow();
        ReflectionTestUtils.setField(refreshToken, "expiresAt", Instant.EPOCH);

        assertThatThrownBy(() -> authCommandService.reissue(tokens.refreshToken()))
                .isInstanceOf(ExpiredRefreshTokenException.class);
    }
}
