package kr.omong.dulpick.domain.member.application.query.reader;

import kr.omong.dulpick.domain.auth.domain.SocialAccount;
import kr.omong.dulpick.domain.auth.domain.SocialAccountRepository;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.member.application.query.view.MemberProfileView;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.member.domain.MemberProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemberProfileReaderTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final SocialAccountRepository socialAccountRepository =
            mock(SocialAccountRepository.class);
    private final MemberProfileRepository memberProfileRepository =
            mock(MemberProfileRepository.class);
    private final MemberProfileReader reader = new MemberProfileReader(
            memberRepository,
            socialAccountRepository,
            memberProfileRepository
    );

    @Test
    void returnsProfileViewWithoutInternalAuthenticationValues() {
        Member member = Member.create();
        ReflectionTestUtils.setField(member, "id", 1L);
        SocialAccount account = SocialAccount.create(
                member,
                SocialProvider.APPLE,
                "provider-subject",
                "member@example.com"
        );
        account.updateProviderAuthorization("encrypted-refresh-token", "com.dulpick.app");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(socialAccountRepository.findAllByMemberId(1L)).thenReturn(List.of(account));

        MemberProfileView profile = reader.read(1L);

        assertThat(profile.memberId()).isEqualTo(1L);
        assertThat(profile.status()).isEqualTo(member.getStatus());
        assertThat(profile.onboardingCompleted()).isFalse();
        assertThat(profile.nickname()).isNull();
        assertThat(profile.socialAccounts()).singleElement()
                .satisfies(socialAccount -> {
                    assertThat(socialAccount.provider()).isEqualTo(SocialProvider.APPLE);
                    assertThat(socialAccount.email()).isEqualTo("member@example.com");
                });
    }
}
