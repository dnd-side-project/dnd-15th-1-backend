package kr.omong.dulpick.domain.auth.application;

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
class TokenServiceTest {

    @Autowired
    private TokenService tokenService;

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

        IssuedTokens rotatedTokens = tokenService.rotate(issuedTokens.refreshToken());
        IssuedTokens otherSessionTokens = tokenService.issue(member);

        assertThat(rotatedTokens.refreshToken()).isNotEqualTo(issuedTokens.refreshToken());
        assertThatThrownBy(() -> tokenService.rotate(issuedTokens.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> tokenService.rotate(rotatedTokens.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> tokenService.rotate(otherSessionTokens.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void revokesOnlyTokenOwnedByAuthenticatedMember() {
        Member owner = memberRepository.save(Member.create());
        Member otherMember = memberRepository.save(Member.create());
        IssuedTokens issuedTokens = tokenService.issue(owner);

        tokenService.revoke(issuedTokens.refreshToken(), otherMember.getId());
        IssuedTokens rotatedTokens = tokenService.rotate(issuedTokens.refreshToken());
        tokenService.revoke(rotatedTokens.refreshToken(), owner.getId());

        assertThatThrownBy(() -> tokenService.rotate(rotatedTokens.refreshToken()))
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

        assertThatThrownBy(() -> tokenService.rotate(tokens.refreshToken()))
                .isInstanceOf(ExpiredRefreshTokenException.class);
    }
}
