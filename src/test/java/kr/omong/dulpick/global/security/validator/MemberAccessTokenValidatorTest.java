package kr.omong.dulpick.global.security.validator;

import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemberAccessTokenValidatorTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final MemberAccessTokenValidator validator =
            new MemberAccessTokenValidator(memberRepository);

    @Test
    void acceptsActiveMemberWithCurrentTokenVersion() {
        Member member = Member.create(Instant.EPOCH);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        OAuth2TokenValidatorResult result = validator.validate(jwt(1L, 0));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void rejectsTokenIssuedBeforeWithdrawal() {
        Member member = Member.create(Instant.EPOCH);
        member.withdraw(Instant.parse("2026-07-27T00:00:00Z"));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        OAuth2TokenValidatorResult result = validator.validate(jwt(1L, 0));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void rejectsTokenForWithdrawnMemberEvenWithCurrentTokenVersion() {
        Member member = Member.create(Instant.EPOCH);
        member.withdraw(Instant.parse("2026-07-27T00:00:00Z"));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        OAuth2TokenValidatorResult result = validator.validate(jwt(1L, 1));

        assertThat(result.hasErrors()).isTrue();
    }

    private Jwt jwt(Long memberId, long tokenVersion) {
        Instant issuedAt = Instant.now();
        return new Jwt(
                "access-token",
                issuedAt,
                issuedAt.plusSeconds(300),
                Map.of("alg", "HS256"),
                Map.of(
                        "sub", memberId.toString(),
                        "tokenVersion", tokenVersion
                )
        );
    }
}
