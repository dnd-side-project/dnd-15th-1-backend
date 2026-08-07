package kr.omong.dulpick.domain.member;

import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.support.SocialAccountService;
import kr.omong.dulpick.domain.auth.application.support.TokenService;
import kr.omong.dulpick.domain.auth.application.support.model.ProviderAuthorization;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeRepository;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeStatus;
import kr.omong.dulpick.domain.member.domain.DatePreferenceOption;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberProfile;
import kr.omong.dulpick.domain.member.domain.MemberProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemberProfileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SocialAccountService socialAccountService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private MemberProfileRepository memberProfileRepository;

    @Autowired
    private ConnectionCodeRepository connectionCodeRepository;

    @Test
    void initializesProfilePreferencesAndConnectionCodeAtomically() throws Exception {
        AuthenticatedTestMember testMember = createMember();

        mockMvc.perform(post("/api/v1/members/me/profile")
                        .header("Authorization", bearer(testMember.tokens()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initializationRequest(1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nickname").value("둘픽이"))
                .andExpect(jsonPath("$.profileIcon").value(1))
                .andExpect(jsonPath("$.datePreferences.indoorOutdoor").value("INDOOR"))
                .andExpect(jsonPath("$.connectionCode", matchesPattern("^[A-Z]{6}$")))
                .andExpect(jsonPath("$.shareUrl", matchesPattern(".+[A-Z]{6}$")));

        MemberProfile profile = memberProfileRepository.findById(
                testMember.member().getId()
        ).orElseThrow();
        assertThat(profile.getNickname()).isEqualTo("둘픽이");
        assertThat(connectionCodeRepository.findByMemberIdAndStatus(
                testMember.member().getId(),
                ConnectionCodeStatus.ACTIVE
        )).isPresent();

        mockMvc.perform(get("/api/v1/members/me")
                        .header("Authorization", bearer(testMember.tokens())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboardingCompleted").value(true))
                .andExpect(jsonPath("$.profileIcon").value(1))
                .andExpect(jsonPath("$.datePreferences.dateTime").value("NIGHT"));

        mockMvc.perform(get("/api/v1/connection-codes/me")
                        .header("Authorization", bearer(testMember.tokens())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", matchesPattern("^[A-Z]{6}$")));
    }

    @Test
    void rejectsInvalidIconAndIncompleteDatePreferences() throws Exception {
        AuthenticatedTestMember invalidIconMember = createMember();

        mockMvc.perform(post("/api/v1/members/me/profile")
                        .header("Authorization", bearer(invalidIconMember.tokens()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initializationRequest(0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        assertThat(memberProfileRepository.existsById(
                invalidIconMember.member().getId()
        )).isFalse();

        AuthenticatedTestMember incompleteMember = createMember();
        mockMvc.perform(post("/api/v1/members/me/profile")
                        .header("Authorization", bearer(incompleteMember.tokens()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname":"둘픽이",
                                  "profileIcon":1,
                                  "datePreferences":{
                                    "indoorOutdoor":"INDOOR",
                                    "activityLevel":"ACTIVE",
                                    "dateTime":"NIGHT"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void rejectsDatePreferenceFromAnotherCategory() throws Exception {
        AuthenticatedTestMember testMember = createMember();

        mockMvc.perform(post("/api/v1/members/me/profile")
                        .header("Authorization", bearer(testMember.tokens()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname":"둘픽이",
                                  "profileIcon":1,
                                  "datePreferences":{
                                    "indoorOutdoor":"ACTIVE",
                                    "activityLevel":"INDOOR",
                                    "dateTime":"FOOD",
                                    "dateFocus":"NIGHT"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        assertThat(memberProfileRepository.existsById(testMember.member().getId())).isFalse();
    }

    @Test
    void updatesMyPageProfileAndAllDatePreferences() throws Exception {
        AuthenticatedTestMember testMember = createMember();
        initialize(testMember);

        mockMvc.perform(patch("/api/v1/members/me/profile")
                        .header("Authorization", bearer(testMember.tokens()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"새닉네임","profileIcon":5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("새닉네임"))
                .andExpect(jsonPath("$.profileIcon").value(5));

        mockMvc.perform(put("/api/v1/members/me/date-preferences")
                        .header("Authorization", bearer(testMember.tokens()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "indoorOutdoor":"OUTDOOR",
                                  "activityLevel":"STATIC",
                                  "dateTime":"DAY",
                                  "dateFocus":"SIGHTSEEING"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indoorOutdoor").value("OUTDOOR"))
                .andExpect(jsonPath("$.activityLevel").value("STATIC"));

        MemberProfile profile = memberProfileRepository.findById(
                testMember.member().getId()
        ).orElseThrow();
        assertThat(profile.getNickname()).isEqualTo("새닉네임");
        assertThat(profile.getProfileIcon()).isEqualTo(5);
        assertThat(profile.getDatePreferences().indoorOutdoor())
                .isEqualTo(DatePreferenceOption.OUTDOOR);
        assertThat(profile.getDatePreferences().activityLevel())
                .isEqualTo(DatePreferenceOption.STATIC);
        assertThat(profile.getDatePreferences().dateTime())
                .isEqualTo(DatePreferenceOption.DAY);
        assertThat(profile.getDatePreferences().dateFocus())
                .isEqualTo(DatePreferenceOption.SIGHTSEEING);
    }

    @Test
    void rejectsDuplicateInitializationAndEmptyPatch() throws Exception {
        AuthenticatedTestMember testMember = createMember();
        initialize(testMember);

        mockMvc.perform(post("/api/v1/members/me/profile")
                        .header("Authorization", bearer(testMember.tokens()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initializationRequest(2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROFILE_ALREADY_INITIALIZED"));

        mockMvc.perform(patch("/api/v1/members/me/profile")
                        .header("Authorization", bearer(testMember.tokens()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    private void initialize(AuthenticatedTestMember testMember) throws Exception {
        mockMvc.perform(post("/api/v1/members/me/profile")
                        .header("Authorization", bearer(testMember.tokens()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initializationRequest(1)))
                .andExpect(status().isCreated());
    }

    private String initializationRequest(int profileIcon) {
        return """
                {
                  "nickname":"둘픽이",
                  "profileIcon":%d,
                  "datePreferences":{
                    "indoorOutdoor":"INDOOR",
                    "activityLevel":"ACTIVE",
                    "dateTime":"NIGHT",
                    "dateFocus":"FOOD"
                  }
                }
                """.formatted(profileIcon);
    }

    private AuthenticatedTestMember createMember() {
        String providerSubject = "profile-" + UUID.randomUUID();
        Member member = socialAccountService.getOrCreate(
                SocialProvider.KAKAO,
                providerSubject,
                providerSubject + "@example.com",
                ProviderAuthorization.none()
        ).member();
        return new AuthenticatedTestMember(member, tokenService.issue(member));
    }

    private String bearer(IssuedTokens tokens) {
        return "Bearer " + tokens.accessToken();
    }

    private record AuthenticatedTestMember(Member member, IssuedTokens tokens) {
    }
}
