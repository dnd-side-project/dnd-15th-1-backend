package kr.omong.dulpick.domain.notification;

import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.support.SocialAccountService;
import kr.omong.dulpick.domain.auth.application.support.TokenService;
import kr.omong.dulpick.domain.auth.application.support.model.ProviderAuthorization;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.couple.domain.event.CoupleConnectedEvent;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.notification.application.PushDeviceService;
import kr.omong.dulpick.domain.notification.application.RegisterPushDeviceCommand;
import kr.omong.dulpick.domain.notification.domain.NotificationDeliveryRepository;
import kr.omong.dulpick.domain.notification.domain.NotificationRepository;
import kr.omong.dulpick.domain.notification.domain.PushPlatform;
import kr.omong.dulpick.domain.notification.domain.PushProviderType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties =
        "notification.push.registration-encryption-key="
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
)
@AutoConfigureMockMvc
class NotificationInboxIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SocialAccountService socialAccountService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PushDeviceService pushDeviceService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationDeliveryRepository deliveryRepository;

    @Test
    void createsNotificationsBeforeCommitAndSupportsInboxReadContract() throws Exception {
        TestMember first = createMember();
        TestMember second = createMember();
        registerDevice(first.memberId());

        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(
                new CoupleConnectedEvent(
                        900_001L,
                        first.memberId(),
                        second.memberId(),
                        Instant.parse("2026-08-07T09:00:00Z")
                )
        ));

        assertThat(notificationRepository.findPage(
                first.memberId(),
                null,
                org.springframework.data.domain.PageRequest.of(0, 10)
        )).hasSize(1);
        assertThat(notificationRepository.findPage(
                second.memberId(),
                null,
                org.springframework.data.domain.PageRequest.of(0, 10)
        )).hasSize(1);
        assertThat(deliveryRepository.countByReceiverMemberId(first.memberId()))
                .isEqualTo(1);
        Long secondNotificationId = notificationRepository.findPage(
                second.memberId(),
                null,
                org.springframework.data.domain.PageRequest.of(0, 1)
        ).getFirst().getId();

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", bearer(first.tokens())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications[0].type")
                        .value("COUPLE_CONNECTED"))
                .andExpect(jsonPath("$.notifications[0].route")
                        .value("COUPLE_STATUS"))
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));

        mockMvc.perform(patch(
                        "/api/v1/notifications/{notificationId}/read",
                        secondNotificationId
                ).header("Authorization", bearer(first.tokens())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));

        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .header("Authorization", bearer(first.tokens())))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", bearer(first.tokens())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0))
                .andExpect(jsonPath("$.notifications[0].read").value(true));

        mockMvc.perform(get("/api/v1/notifications")
                        .param("cursor", "invalid")
                        .header("Authorization", bearer(first.tokens())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("cursor"));
    }

    private void registerDevice(Long memberId) {
        pushDeviceService.register(memberId, new RegisterPushDeviceCommand(
                UUID.randomUUID(),
                PushPlatform.IOS,
                PushProviderType.FCM,
                "notification-inbox-token-" + UUID.randomUUID(),
                "1.0.0"
        ));
    }

    private TestMember createMember() {
        String subject = "notification-inbox-" + UUID.randomUUID();
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
