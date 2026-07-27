package kr.omong.dulpick.domain.auth.application;

import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.transaction.annotation.Transactional;

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
    void rotatesRefreshTokenOnlyOnce() {
        Member member = memberRepository.save(Member.create());
        IssuedTokens issuedTokens = tokenService.issue(member);

        IssuedTokens rotatedTokens = tokenService.rotate(issuedTokens.refreshToken());

        assertThat(rotatedTokens.refreshToken()).isNotEqualTo(issuedTokens.refreshToken());
        assertThatThrownBy(() -> tokenService.rotate(issuedTokens.refreshToken()))
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
}
