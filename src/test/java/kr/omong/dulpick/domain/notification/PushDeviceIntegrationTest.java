package kr.omong.dulpick.domain.notification;

import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.support.SocialAccountService;
import kr.omong.dulpick.domain.auth.application.support.TokenService;
import kr.omong.dulpick.domain.auth.application.support.model.ProviderAuthorization;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.member.application.command.MemberCommandService;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.notification.domain.PushDeviceRepository;
import kr.omong.dulpick.domain.notification.domain.PushDeviceStatus;
import kr.omong.dulpick.domain.notification.infrastructure.PushRegistrationCipher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties =
        "notification.push.registration-encryption-key="
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
)
@AutoConfigureMockMvc
@Transactional
class PushDeviceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SocialAccountService socialAccountService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private MemberCommandService memberCommandService;

    @Autowired
    private PushDeviceRepository pushDeviceRepository;

    @Autowired
    private PushRegistrationCipher registrationCipher;

    @Test
    void registersRefreshesAndUnregistersCurrentDevice() throws Exception {
        TestMember member = createMember();
        UUID deviceId = UUID.randomUUID();

        register(member, deviceId, "first-token")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value(deviceId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        register(member, deviceId, "rotated-token").andExpect(status().isOk());

        assertThat(pushDeviceRepository.findAllByMemberId(member.memberId()))
                .singleElement()
                .satisfies(device -> assertThat(registrationCipher.decrypt(
                        device.getEncryptedRegistrationId()
                )).isEqualTo("rotated-token"));

        mockMvc.perform(delete("/api/v1/push-devices/{deviceId}", deviceId)
                        .header("Authorization", bearer(member.tokens())))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/push-devices/{deviceId}", deviceId)
                        .header("Authorization", bearer(member.tokens())))
                .andExpect(status().isNoContent());
        assertThat(pushDeviceRepository.findAllByMemberId(member.memberId()))
                .singleElement()
                .satisfies(device -> assertThat(device.getStatus())
                        .isEqualTo(PushDeviceStatus.LOGGED_OUT));
    }

    @Test
    void rejectsUnknownDeviceAndDisablesAllOnWithdrawal() throws Exception {
        TestMember member = createMember();
        register(member, UUID.randomUUID(), "first-token").andExpect(status().isOk());
        register(member, UUID.randomUUID(), "second-token").andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/push-devices/{deviceId}", UUID.randomUUID())
                        .header("Authorization", bearer(member.tokens())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PUSH_DEVICE_NOT_FOUND"));

        memberCommandService.withdraw(member.memberId());

        assertThat(pushDeviceRepository.findAllByMemberIdAndStatus(
                member.memberId(),
                PushDeviceStatus.WITHDRAWN
        )).hasSize(2);
    }

    @Test
    void transfersRotatedRegistrationToLatestMemberAndDevice() throws Exception {
        TestMember previousMember = createMember();
        TestMember currentMember = createMember();
        UUID previousDeviceId = UUID.randomUUID();
        UUID currentDeviceId = UUID.randomUUID();

        register(previousMember, previousDeviceId, "shared-token")
                .andExpect(status().isOk());
        register(currentMember, currentDeviceId, "shared-token")
                .andExpect(status().isOk());

        assertThat(pushDeviceRepository.findAllByMemberId(currentMember.memberId()))
                .singleElement()
                .satisfies(device -> {
                    assertThat(device.getMemberId()).isEqualTo(currentMember.memberId());
                    assertThat(device.getDeviceId()).isEqualTo(currentDeviceId.toString());
                    assertThat(device.getStatus()).isEqualTo(PushDeviceStatus.ACTIVE);
                });
    }

    @Test
    void rejectsUnsupportedPushProvider() throws Exception {
        TestMember member = createMember();

        mockMvc.perform(put("/api/v1/push-devices/{deviceId}", UUID.randomUUID())
                        .header("Authorization", bearer(member.tokens()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "platform":"IOS",
                                  "provider":"APNS",
                                  "providerRegistrationId":"apns-token"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("provider"));
    }

    private org.springframework.test.web.servlet.ResultActions register(
            TestMember member,
            UUID deviceId,
            String token
    ) throws Exception {
        return mockMvc.perform(put("/api/v1/push-devices/{deviceId}", deviceId)
                .header("Authorization", bearer(member.tokens()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "platform":"IOS",
                          "provider":"FCM",
                          "providerRegistrationId":"%s",
                          "appVersion":"1.0.0"
                        }
                        """.formatted(token)));
    }

    private TestMember createMember() {
        String subject = "push-device-" + UUID.randomUUID();
        Member member = socialAccountService.getOrCreate(
                SocialProvider.KAKAO,
                subject,
                subject + "@example.com",
                ProviderAuthorization.none()
        ).member();
        return new TestMember(member.getId(), tokenService.issue(member));
    }

    private String bearer(IssuedTokens tokens) {
        return "Bearer " + tokens.accessToken();
    }

    private record TestMember(Long memberId, IssuedTokens tokens) {
    }
}
