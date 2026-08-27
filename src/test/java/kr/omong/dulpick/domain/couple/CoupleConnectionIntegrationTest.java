package kr.omong.dulpick.domain.couple;

import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.support.SocialAccountService;
import kr.omong.dulpick.domain.auth.application.support.TokenService;
import kr.omong.dulpick.domain.auth.application.support.model.ProviderAuthorization;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMemberRepository;
import kr.omong.dulpick.domain.couple.domain.ConnectionCode;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeRepository;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeStatus;
import kr.omong.dulpick.domain.couple.domain.CoupleRepository;
import kr.omong.dulpick.domain.couple.domain.CoupleStatus;
import kr.omong.dulpick.domain.couple.domain.event.CoupleConnectedEvent;
import kr.omong.dulpick.domain.couple.domain.event.CoupleDisconnectedEvent;
import kr.omong.dulpick.domain.member.application.command.InitializeMemberProfileCommand;
import kr.omong.dulpick.domain.member.application.command.MemberCommandService;
import kr.omong.dulpick.domain.member.application.command.UpdateMemberProfileCommand;
import kr.omong.dulpick.domain.member.domain.DatePreferenceOption;
import kr.omong.dulpick.domain.member.domain.DatePreferences;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.member.domain.MemberStatus;
import kr.omong.dulpick.global.security.crypto.Sha256;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
@Transactional
class CoupleConnectionIntegrationTest {

    private static final DatePreferences PREFERENCES = new DatePreferences(
            DatePreferenceOption.INDOOR,
            DatePreferenceOption.ACTIVE,
            DatePreferenceOption.NIGHT,
            DatePreferenceOption.FOOD
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SocialAccountService socialAccountService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private MemberCommandService memberCommandService;

    @Autowired
    private CoupleRepository coupleRepository;

    @Autowired
    private ActiveCoupleMemberRepository activeCoupleMemberRepository;

    @Autowired
    private ConnectionCodeRepository connectionCodeRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Test
    void connectsTwoMembersWithOneStatusContract() throws Exception {
        TestMember inviter = createProfileMember("초대자", 3);
        TestMember requester = createProfileMember("요청자", 1);

        mockMvc.perform(post("/api/v1/couples")
                        .header("Authorization", bearer(requester))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(connectionRequest(inviter.connectionCode())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.me.nickname").value("요청자"))
                .andExpect(jsonPath("$.partner.nickname").value("초대자"))
                .andExpect(jsonPath("$.connectedAt").isNotEmpty())
                .andExpect(jsonPath("$.daysTogether").value(1));

        assertConnectionCodeStatus(inviter, ConnectionCodeStatus.USED);
        assertConnectionCodeStatus(requester, ConnectionCodeStatus.REVOKED);

        Long coupleId = activeCoupleMemberRepository
                .findByMemberId(inviter.member().getId())
                .orElseThrow()
                .getCouple()
                .getId();
        assertThat(coupleRepository.findById(coupleId)).isPresent();
        assertThat(activeCoupleMemberRepository.findByMemberId(requester.member().getId()))
                .isPresent();
        assertThat(applicationEvents.stream(CoupleConnectedEvent.class)).hasSize(1);

        mockMvc.perform(get("/api/v1/couples/me")
                        .header("Authorization", bearer(inviter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.me.nickname").value("초대자"))
                .andExpect(jsonPath("$.partner.nickname").value("요청자"))
                .andExpect(jsonPath("$.daysTogether").value(1));

        mockMvc.perform(get("/api/v1/connection-codes/me")
                        .header("Authorization", bearer(requester)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEMBER_ALREADY_CONNECTED"));
    }

    @Test
    void connectsMembersBeforeDatePreferencesAreSet() throws Exception {
        TestMember inviter = createProfileMember("성향전", 1, null);
        TestMember requester = createProfileMember("성향후", 2, null);

        mockMvc.perform(post("/api/v1/couples")
                        .header("Authorization", bearer(requester))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(connectionRequest(inviter.connectionCode())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.partner.nickname").value("성향전"))
                .andExpect(jsonPath("$.partner.profileIcon").value(1));
    }

    @Test
    void returnsDisconnectedStatusWithOnlyMyProfile() throws Exception {
        TestMember member = createProfileMember("미연결", 2);

        mockMvc.perform(get("/api/v1/couples/me")
                        .header("Authorization", bearer(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(false))
                .andExpect(jsonPath("$.me.nickname").value("미연결"))
                .andExpect(jsonPath("$.me.profileIcon").value(2))
                .andExpect(jsonPath("$.partner").doesNotExist())
                .andExpect(jsonPath("$.connectedAt").doesNotExist())
                .andExpect(jsonPath("$.daysTogether").doesNotExist());
    }

    @Test
    void reflectsLatestPartnerProfileAfterConnection() throws Exception {
        TestMember inviter = createProfileMember("수정전", 1);
        TestMember requester = createProfileMember("요청자", 2);
        connect(requester, inviter.connectionCode());

        memberCommandService.updateProfile(
                inviter.member().getId(),
                new UpdateMemberProfileCommand("수정후", 5)
        );

        mockMvc.perform(get("/api/v1/couples/me")
                        .header("Authorization", bearer(requester)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partner.nickname").value("수정후"))
                .andExpect(jsonPath("$.partner.profileIcon").value(5));
    }

    @Test
    void rejectsSelfInvalidAndAlreadyConnectedRequests() throws Exception {
        TestMember first = createProfileMember("첫번째", 1);
        TestMember second = createProfileMember("두번째", 2);
        TestMember third = createProfileMember("세번째", 3);

        mockMvc.perform(post("/api/v1/couples")
                        .header("Authorization", bearer(first))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(connectionRequest(first.connectionCode())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SELF_CONNECTION_NOT_ALLOWED"));

        mockMvc.perform(post("/api/v1/couples")
                        .header("Authorization", bearer(first))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(connectionRequest("12!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CONNECTION_CODE"));

        connect(first, second.connectionCode());

        mockMvc.perform(post("/api/v1/couples")
                        .header("Authorization", bearer(second))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(connectionRequest(third.connectionCode())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEMBER_ALREADY_CONNECTED"));
    }

    @Test
    void disconnectsCoupleAndIssuesNewCodesOnlyOnce() throws Exception {
        TestMember first = createProfileMember("첫번째", 1);
        TestMember second = createProfileMember("두번째", 2);
        connect(first, second.connectionCode());
        Long coupleId = activeCoupleMemberRepository
                .findByMemberId(first.member().getId())
                .orElseThrow()
                .getCouple()
                .getId();

        mockMvc.perform(delete("/api/v1/couples/me")
                        .header("Authorization", bearer(first)))
                .andExpect(status().isNoContent());

        assertThat(activeCoupleMemberRepository.findByMemberId(first.member().getId()))
                .isEmpty();
        assertThat(activeCoupleMemberRepository.findByMemberId(second.member().getId()))
                .isEmpty();
        assertThat(coupleRepository.findById(coupleId))
                .get()
                .extracting(kr.omong.dulpick.domain.couple.domain.Couple::getStatus)
                .isEqualTo(CoupleStatus.DISCONNECTED);
        assertThat(activeCodeDigest(first)).isNotEqualTo(Sha256.hex(first.connectionCode()));
        assertThat(activeCodeDigest(second)).isNotEqualTo(Sha256.hex(second.connectionCode()));
        assertThat(applicationEvents.stream(CoupleDisconnectedEvent.class))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.reason())
                            .isEqualTo(CoupleDisconnectedEvent.Reason.USER_REQUEST);
                    assertThat(event.requestedByMemberId()).isEqualTo(first.member().getId());
                });

        mockMvc.perform(get("/api/v1/couples/me")
                        .header("Authorization", bearer(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(false));

        mockMvc.perform(delete("/api/v1/couples/me")
                        .header("Authorization", bearer(first)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COUPLE_NOT_FOUND"));
        assertThat(connectionCodeRepository.findAllByMemberId(first.member().getId()))
                .filteredOn(code -> code.getStatus() == ConnectionCodeStatus.ACTIVE)
                .hasSize(1);
    }

    @Test
    void disconnectsCoupleDuringWithdrawalAndIssuesCodeOnlyToPartner() throws Exception {
        TestMember withdrawing = createProfileMember("탈퇴자", 1);
        TestMember partner = createProfileMember("상대방", 2);
        connect(withdrawing, partner.connectionCode());
        Long coupleId = activeCoupleMemberRepository
                .findByMemberId(withdrawing.member().getId())
                .orElseThrow()
                .getCouple()
                .getId();

        mockMvc.perform(delete("/api/v1/members/me")
                        .header("Authorization", bearer(withdrawing)))
                .andExpect(status().isNoContent());

        Member withdrawnMember = memberRepository.findById(withdrawing.member().getId())
                .orElseThrow();
        assertThat(withdrawnMember.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(activeCoupleMemberRepository.findByMemberId(withdrawing.member().getId()))
                .isEmpty();
        assertThat(activeCoupleMemberRepository.findByMemberId(partner.member().getId()))
                .isEmpty();
        assertThat(coupleRepository.findById(coupleId))
                .get()
                .extracting(kr.omong.dulpick.domain.couple.domain.Couple::getStatus)
                .isEqualTo(CoupleStatus.DISCONNECTED);
        assertThat(connectionCodeRepository.findByMemberIdAndStatus(
                withdrawing.member().getId(),
                ConnectionCodeStatus.ACTIVE
        )).isEmpty();
        assertThat(activeCodeDigest(partner))
                .isNotEqualTo(Sha256.hex(partner.connectionCode()));
        assertThat(applicationEvents.stream(CoupleDisconnectedEvent.class))
                .singleElement()
                .satisfies(event -> assertThat(event.reason())
                        .isEqualTo(CoupleDisconnectedEvent.Reason.MEMBER_WITHDRAWAL));

        mockMvc.perform(get("/api/v1/couples/me")
                        .header("Authorization", bearer(partner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(false));
    }

    @Test
    void rejoinDoesNotRestorePreviousCoupleRelationship() throws Exception {
        TestMember withdrawing = createProfileMember("탈퇴자", 1);
        TestMember partner = createProfileMember("이전상대", 2);
        connect(withdrawing, partner.connectionCode());
        Long previousCoupleId = activeCoupleMemberRepository
                .findByMemberId(withdrawing.member().getId())
                .orElseThrow()
                .getCouple()
                .getId();
        memberCommandService.withdraw(withdrawing.member().getId());

        Member rejoined = socialAccountService.getOrCreate(
                SocialProvider.KAKAO,
                withdrawing.providerSubject(),
                "rejoined@example.com",
                ProviderAuthorization.none()
        ).member();

        assertThat(rejoined.isActive()).isTrue();
        assertThat(coupleRepository.findById(previousCoupleId).orElseThrow().getStatus())
                .isEqualTo(CoupleStatus.DISCONNECTED);
        assertThat(activeCoupleMemberRepository.findByMemberId(rejoined.getId())).isEmpty();
        assertThat(activeCoupleMemberRepository.findByMemberId(partner.member().getId()))
                .isEmpty();
    }

    private void connect(TestMember requester, String code) throws Exception {
        mockMvc.perform(post("/api/v1/couples")
                        .header("Authorization", bearer(requester))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(connectionRequest(code)))
                .andExpect(status().isCreated());
    }

    private TestMember createProfileMember(String nickname, int profileIcon) {
        return createProfileMember(nickname, profileIcon, PREFERENCES);
    }

    private TestMember createProfileMember(
            String nickname,
            int profileIcon,
            DatePreferences preferences
    ) {
        String subject = "couple-" + UUID.randomUUID();
        Member member = socialAccountService.getOrCreate(
                SocialProvider.KAKAO,
                subject,
                subject + "@example.com",
                ProviderAuthorization.none()
        ).member();
        IssuedTokens tokens = tokenService.issue(member);
        String code = memberCommandService.initializeProfile(
                member.getId(),
                new InitializeMemberProfileCommand(nickname, profileIcon, preferences)
        ).connectionCode().code();
        return new TestMember(member, tokens, code, subject);
    }

    private String connectionRequest(String code) {
        return """
                {"connectionCode":"%s"}
                """.formatted(code);
    }

    private String bearer(TestMember member) {
        return "Bearer " + member.tokens().accessToken();
    }

    private String activeCodeDigest(TestMember member) {
        return connectionCodeRepository.findByMemberIdAndStatus(
                        member.member().getId(),
                        ConnectionCodeStatus.ACTIVE
                )
                .map(code -> code.getCodeDigest())
                .orElseThrow();
    }

    private void assertConnectionCodeStatus(
            TestMember member,
            ConnectionCodeStatus expectedStatus
    ) {
        List<ConnectionCode> codes = connectionCodeRepository.findAllByMemberId(
                member.member().getId()
        );
        assertThat(codes)
                .filteredOn(code -> code.getStatus() == ConnectionCodeStatus.ACTIVE)
                .isEmpty();
        assertThat(codes)
                .filteredOn(code -> code.getCodeDigest().equals(
                        Sha256.hex(member.connectionCode())
                ))
                .singleElement()
                .satisfies(code -> assertThat(code.getStatus()).isEqualTo(expectedStatus));
    }

    private record TestMember(
            Member member,
            IssuedTokens tokens,
            String connectionCode,
            String providerSubject
    ) {
    }
}
