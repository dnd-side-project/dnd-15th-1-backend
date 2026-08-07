package kr.omong.dulpick.domain.notification;

import kr.omong.dulpick.domain.auth.application.support.SocialAccountService;
import kr.omong.dulpick.domain.auth.application.support.model.ProviderAuthorization;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.couple.domain.event.CoupleConnectedEvent;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.notification.application.NotificationDeliveryClaimService;
import kr.omong.dulpick.domain.notification.application.NotificationDeliveryResultService;
import kr.omong.dulpick.domain.notification.application.NotificationDeliveryWorker;
import kr.omong.dulpick.domain.notification.application.PushDeviceService;
import kr.omong.dulpick.domain.notification.application.RegisterPushDeviceCommand;
import kr.omong.dulpick.domain.notification.config.PushProperties;
import kr.omong.dulpick.domain.notification.domain.NotificationDelivery;
import kr.omong.dulpick.domain.notification.domain.NotificationDeliveryRepository;
import kr.omong.dulpick.domain.notification.domain.NotificationDeliveryStatus;
import kr.omong.dulpick.domain.notification.domain.PushDeviceStatus;
import kr.omong.dulpick.domain.notification.domain.PushPlatform;
import kr.omong.dulpick.domain.notification.domain.PushProviderType;
import kr.omong.dulpick.domain.notification.infrastructure.PushMessage;
import kr.omong.dulpick.domain.notification.infrastructure.PushMessageProvider;
import kr.omong.dulpick.domain.notification.infrastructure.PushRegistrationCipher;
import kr.omong.dulpick.domain.notification.infrastructure.PushSendException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties =
        "notification.push.registration-encryption-key="
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
)
class NotificationDeliveryWorkerIntegrationTest {

    private final List<Long> testMemberIds = new ArrayList<>();

    @Autowired
    private SocialAccountService socialAccountService;

    @Autowired
    private PushDeviceService pushDeviceService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private NotificationDeliveryClaimService claimService;

    @Autowired
    private NotificationDeliveryResultService resultService;

    @Autowired
    private PushRegistrationCipher registrationCipher;

    @Autowired
    private NotificationDeliveryRepository deliveryRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        new NotificationTestDataCleaner(jdbcTemplate).clean(testMemberIds, List.of());
    }

    @Test
    void sendsOutsideDatabaseTransactionAndRecordsProviderMessageId() {
        Long memberId = createDelivery();
        TransactionCheckingProvider provider = new TransactionCheckingProvider();
        NotificationDeliveryWorker worker = worker(provider);

        worker.process(delivery(memberId).getId());

        NotificationDelivery delivery = delivery(memberId);
        assertThat(provider.transactionActive).isFalse();
        assertThat(provider.registrationId).startsWith("delivery-token-");
        assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(delivery.getProviderMessageId()).isEqualTo("fcm-message-id");
        assertThat(delivery.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void retriesTemporaryFailureAndInvalidatesUnregisteredToken() {
        Long retryMemberId = createDelivery();
        NotificationDeliveryWorker retryWorker = worker((registrationId, message) -> {
            throw new PushSendException("UNAVAILABLE", true, false, null);
        });
        retryWorker.process(delivery(retryMemberId).getId());

        NotificationDelivery retryDelivery = delivery(retryMemberId);
        assertThat(retryDelivery.getStatus())
                .isEqualTo(NotificationDeliveryStatus.RETRY_PENDING);
        assertThat(retryDelivery.getAttemptCount()).isEqualTo(1);
        assertThat(retryDelivery.getLastErrorCode()).isEqualTo("UNAVAILABLE");

        Long invalidMemberId = createDelivery();
        NotificationDeliveryWorker invalidWorker = worker((registrationId, message) -> {
            throw new PushSendException("UNREGISTERED", false, true, null);
        });
        invalidWorker.process(delivery(invalidMemberId).getId());

        NotificationDelivery invalidDelivery = delivery(invalidMemberId);
        assertThat(invalidDelivery.getStatus())
                .isEqualTo(NotificationDeliveryStatus.FAILED);
        assertThat(invalidDelivery.getPushDeviceStatus())
                .isEqualTo(PushDeviceStatus.INVALIDATED);
    }

    @Test
    void invalidatesDeviceWhenRegistrationTokenCannotBeDecrypted() {
        Long memberId = createDelivery();
        PushRegistrationCipher mismatchedCipher = new PushRegistrationCipher(
                new PushProperties(differentEncryptionKey()),
                new SecureRandom()
        );
        NotificationDeliveryWorker worker = worker(
                mismatchedCipher,
                (registrationId, message) -> {
                    throw new AssertionError("Push provider must not be called");
                }
        );

        worker.process(delivery(memberId).getId());

        NotificationDelivery delivery = delivery(memberId);
        assertThat(delivery.getStatus()).isEqualTo(NotificationDeliveryStatus.FAILED);
        assertThat(delivery.getLastErrorCode()).isEqualTo("TOKEN_DECRYPTION_FAILED");
        assertThat(delivery.getPushDeviceStatus())
                .isEqualTo(PushDeviceStatus.INVALIDATED);
    }

    private Long createDelivery() {
        Member receiver = createMember();
        Member partner = createMember();
        pushDeviceService.register(receiver.getId(), new RegisterPushDeviceCommand(
                UUID.randomUUID(),
                PushPlatform.IOS,
                PushProviderType.FCM,
                "delivery-token-" + UUID.randomUUID(),
                "1.0.0"
        ));
        long coupleId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(
                new CoupleConnectedEvent(
                        coupleId,
                        receiver.getId(),
                        partner.getId(),
                        Instant.now()
                )
        ));
        return receiver.getId();
    }

    private Member createMember() {
        String subject = "delivery-worker-" + UUID.randomUUID();
        Member member = socialAccountService.getOrCreate(
                SocialProvider.KAKAO,
                subject,
                subject + "@example.com",
                ProviderAuthorization.none()
        ).member();
        testMemberIds.add(member.getId());
        return member;
    }

    private NotificationDelivery delivery(Long memberId) {
        return deliveryRepository.findAllByReceiverMemberId(memberId).getFirst();
    }

    private NotificationDeliveryWorker worker(PushMessageProvider provider) {
        return worker(registrationCipher, provider);
    }

    private NotificationDeliveryWorker worker(
            PushRegistrationCipher cipher,
            PushMessageProvider provider
    ) {
        return new NotificationDeliveryWorker(
                claimService,
                resultService,
                cipher,
                provider
        );
    }

    private String differentEncryptionKey() {
        byte[] key = "12345678901234567890123456789012"
                .getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(key);
    }

    private static class TransactionCheckingProvider implements PushMessageProvider {

        private boolean transactionActive;
        private String registrationId;

        @Override
        public String send(String registrationId, PushMessage message) {
            this.transactionActive = TransactionSynchronizationManager
                    .isActualTransactionActive();
            this.registrationId = registrationId;
            return "fcm-message-id";
        }
    }
}
