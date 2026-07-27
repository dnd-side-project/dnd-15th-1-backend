package kr.omong.dulpick.domain.auth.application;

import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SocialAccountServiceTest {

    @Autowired
    private SocialAccountService socialAccountService;

    @Test
    void returnsExistingMemberForSameProviderSubject() {
        AuthenticatedMember firstLogin = socialAccountService.getOrCreate(
                SocialProvider.GOOGLE,
                "provider-subject",
                "member@example.com",
                null
        );
        AuthenticatedMember secondLogin = socialAccountService.getOrCreate(
                SocialProvider.GOOGLE,
                "provider-subject",
                "updated@example.com",
                null
        );

        assertThat(firstLogin.newMember()).isTrue();
        assertThat(secondLogin.newMember()).isFalse();
        assertThat(secondLogin.member().getId()).isEqualTo(firstLogin.member().getId());
    }
}
