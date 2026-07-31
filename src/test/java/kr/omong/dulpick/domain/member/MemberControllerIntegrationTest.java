package kr.omong.dulpick.domain.member;

import kr.omong.dulpick.domain.auth.application.InvalidRefreshTokenException;
import kr.omong.dulpick.domain.auth.application.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.ProviderAuthorization;
import kr.omong.dulpick.domain.auth.application.SocialAccountService;
import kr.omong.dulpick.domain.auth.application.TokenService;
import kr.omong.dulpick.domain.auth.domain.SocialAccountRepository;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.member.application.command.MemberCommandService;
import kr.omong.dulpick.domain.member.application.exception.MemberAlreadyWithdrawnException;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.member.domain.MemberStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemberControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private SocialAccountService socialAccountService;

    @Autowired
    private MemberCommandService memberCommandService;

    @Autowired
    private TokenService tokenService;

    @Test
    void authenticatedMemberCanViewOwnProfile() throws Exception {
        Member member = createSocialMember("profile-subject");
        IssuedTokens tokens = tokenService.issue(member);

        mockMvc.perform(get("/api/v1/members/me")
                        .header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(member.getId()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt")
                        .value(not(containsString("+09:00"))))
                .andExpect(jsonPath("$.socialAccounts[0].provider").value("KAKAO"))
                .andExpect(jsonPath("$.socialAccounts[0].email")
                        .value("member@example.com"));
    }

    @Test
    void withdrawalKeepsAccountAndRevokesRefreshToken() throws Exception {
        Member member = createSocialMember("withdraw-subject");
        IssuedTokens tokens = tokenService.issue(member);

        mockMvc.perform(delete("/api/v1/members/me")
                        .header("Authorization", bearer(tokens)))
                .andExpect(status().isNoContent());

        Member withdrawnMember = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(withdrawnMember.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(withdrawnMember.getLastWithdrawnAt()).isNotNull();
        assertThat(memberRepository.count()).isPositive();
        assertThat(socialAccountRepository.count()).isPositive();
        assertThatThrownBy(() -> tokenService.rotate(tokens.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);

        mockMvc.perform(get("/api/v1/members/me")
                        .header("Authorization", bearer(tokens)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    void socialLoginReactivatesWithdrawnMember() {
        Member member = createSocialMember("rejoin-subject");
        memberCommandService.withdraw(member.getId());

        Member rejoinedMember = socialAccountService.getOrCreate(
                SocialProvider.KAKAO,
                "rejoin-subject",
                "updated@example.com",
                ProviderAuthorization.none()
        ).member();

        assertThat(rejoinedMember.getId()).isEqualTo(member.getId());
        assertThat(rejoinedMember.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(rejoinedMember.getLastWithdrawnAt()).isNotNull();
        assertThat(rejoinedMember.getLastRejoinedAt()).isNotNull();
    }

    @Test
    void rejectsDuplicateWithdrawal() {
        Member member = createSocialMember("duplicate-withdraw-subject");
        memberCommandService.withdraw(member.getId());

        assertThatThrownBy(() -> memberCommandService.withdraw(member.getId()))
                .isInstanceOf(MemberAlreadyWithdrawnException.class);
    }

    private Member createSocialMember(String providerSubject) {
        return socialAccountService.getOrCreate(
                SocialProvider.KAKAO,
                providerSubject,
                "member@example.com",
                ProviderAuthorization.none()
        ).member();
    }

    private String bearer(IssuedTokens tokens) {
        return "Bearer " + tokens.accessToken();
    }
}
