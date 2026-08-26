package kr.omong.dulpick.domain.auth.application.command;

import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.support.TokenService;
import kr.omong.dulpick.domain.auth.domain.RefreshTokenRepository;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = "auth.jwt.refresh-token-replay-grace=5s")
@Transactional
class RefreshTokenReplayGraceIntegrationTest {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AuthCommandService authCommandService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void returnsSameReplacementForImmediateReplay() {
        Member member = memberRepository.save(Member.create(Instant.EPOCH));
        IssuedTokens initialTokens = tokenService.issue(member);

        IssuedTokens rotatedTokens = authCommandService.reissue(initialTokens.refreshToken());
        IssuedTokens replayedTokens = authCommandService.reissue(initialTokens.refreshToken());

        assertThat(replayedTokens.refreshToken()).isEqualTo(rotatedTokens.refreshToken());
        assertThat(refreshTokenRepository.findAll().stream()
                .filter(token -> member.getId().equals(token.getMember().getId()))
                .count()).isEqualTo(2);
    }
}
