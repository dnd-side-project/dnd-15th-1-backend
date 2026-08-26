package kr.omong.dulpick.domain.notification;

import kr.omong.dulpick.domain.auth.application.support.SocialAccountService;
import kr.omong.dulpick.domain.auth.application.support.model.ProviderAuthorization;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.couple.domain.Couple;
import kr.omong.dulpick.domain.couple.domain.CoupleRepository;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.notification.application.command.NotificationSettingsCommand;
import kr.omong.dulpick.domain.notification.application.command.NotificationSettingsService;
import kr.omong.dulpick.domain.notification.application.command.PushDeviceService;
import kr.omong.dulpick.domain.notification.application.command.RegisterPushDeviceCommand;
import kr.omong.dulpick.domain.notification.application.event.ContentSavedEvent;
import kr.omong.dulpick.domain.notification.application.event.DateCoursePlannedEvent;
import kr.omong.dulpick.domain.notification.application.event.DateScheduleReminderDueEvent;
import kr.omong.dulpick.domain.notification.domain.ContentSaveCounterId;
import kr.omong.dulpick.domain.notification.domain.ContentSaveCounterRepository;
import kr.omong.dulpick.domain.notification.domain.NotificationDeliveryRepository;
import kr.omong.dulpick.domain.notification.domain.NotificationRepository;
import kr.omong.dulpick.domain.notification.domain.PushPlatform;
import kr.omong.dulpick.domain.notification.domain.PushProviderType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties =
        "notification.push.registration-encryption-key="
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
)
class SelectiveNotificationIntegrationTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-08-07T10:00:00Z");

    private final List<Long> testMemberIds = new ArrayList<>();
    private final List<Long> testCoupleIds = new ArrayList<>();

    @Autowired
    private SocialAccountService socialAccountService;

    @Autowired
    private CoupleRepository coupleRepository;

    @Autowired
    private PushDeviceService pushDeviceService;

    @Autowired
    private NotificationSettingsService settingsService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ContentSaveCounterRepository counterRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationDeliveryRepository deliveryRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        new NotificationTestDataCleaner(jdbcTemplate).clean(
                testMemberIds,
                testCoupleIds
        );
    }

    @Test
    void createsEveryTenthContentNotificationWithoutPushWhenSettingIsOff() {
        Member saver = createMember();
        Member partner = createMember();
        Couple couple = coupleRepository.save(Couple.connect(OCCURRED_AT));
        testCoupleIds.add(couple.getId());
        registerDevice(partner.getId());
        settingsService.update(partner.getId(), new NotificationSettingsCommand(
                false,
                true,
                false,
                null
        ));

        for (long contentId = 1; contentId <= 10; contentId++) {
            publish(new ContentSavedEvent(
                    couple.getId(),
                    saver.getId(),
                    partner.getId(),
                    contentId,
                    OCCURRED_AT.plusSeconds(contentId)
            ));
        }

        assertThat(counterRepository.findById(new ContentSaveCounterId(
                couple.getId(),
                saver.getId()
        ))).get().extracting(counter -> counter.getSaveCount()).isEqualTo(10L);
        assertThat(notificationRepository.findPage(
                partner.getId(),
                null,
                PageRequest.of(0, 10)
        )).singleElement().satisfies(notification -> {
            assertThat(notification.getType().name())
                    .isEqualTo("CONTENT_SAVE_MILESTONE");
            assertThat(notification.getBody()).contains("10개");
        });
        assertThat(deliveryRepository.countByReceiverMemberId(partner.getId()))
                .isZero();
    }

    @Test
    void deduplicatesDateReminderPerMemberAndReminderTime() {
        Member receiver = createMember();
        registerDevice(receiver.getId());
        DateScheduleReminderDueEvent event = new DateScheduleReminderDueEvent(
                501L,
                Instant.parse("2026-08-10T09:00:00Z"),
                List.of(receiver.getId()),
                OCCURRED_AT
        );

        publish(event);
        publish(event);

        assertThat(notificationRepository.findPage(
                receiver.getId(),
                null,
                PageRequest.of(0, 10)
        )).hasSize(1);
        assertThat(deliveryRepository.countByReceiverMemberId(receiver.getId()))
                .isEqualTo(1);
    }

    @Test
    void doesNotCreateInboxNotificationForDateCoursePlannedPush() {
        Member planner = createMember();
        Member partner = createMember();
        Couple couple = coupleRepository.save(Couple.connect(OCCURRED_AT));
        testCoupleIds.add(couple.getId());
        registerDevice(partner.getId());

        publish(new DateCoursePlannedEvent(
                1001L,
                couple.getId(),
                planner.getId(),
                partner.getId(),
                "둘픽이",
                "성수동 데이트",
                OCCURRED_AT
        ));

        assertThat(notificationRepository.findPage(
                partner.getId(),
                null,
                PageRequest.of(0, 10)
        )).isEmpty();
        assertThat(deliveryRepository.countByReceiverMemberId(partner.getId()))
                .isZero();
    }

    @Test
    void countsConcurrentContentEventsAndCreatesOneMilestone() throws Exception {
        Member saver = createMember();
        Member partner = createMember();
        Couple couple = coupleRepository.save(Couple.connect(OCCURRED_AT));
        testCoupleIds.add(couple.getId());
        CountDownLatch ready = new CountDownLatch(10);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(10);

        try {
            List<Future<Object>> futures = java.util.stream.LongStream.rangeClosed(1, 10)
                    .mapToObj(contentId -> executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        publish(new ContentSavedEvent(
                                couple.getId(),
                                saver.getId(),
                                partner.getId(),
                                contentId,
                                OCCURRED_AT.plusSeconds(contentId)
                        ));
                        return null;
                    })).toList();
            ready.await();
            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(counterRepository.findById(new ContentSaveCounterId(
                couple.getId(),
                saver.getId()
        ))).get().extracting(counter -> counter.getSaveCount()).isEqualTo(10L);
        assertThat(notificationRepository.findPage(
                partner.getId(),
                null,
                PageRequest.of(0, 10)
        )).hasSize(1);
    }

    private void publish(Object event) {
        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(event));
    }

    private void registerDevice(Long memberId) {
        pushDeviceService.register(memberId, new RegisterPushDeviceCommand(
                UUID.randomUUID(),
                PushPlatform.IOS,
                PushProviderType.FCM,
                "selective-notification-token-" + UUID.randomUUID(),
                "1.0.0"
        ));
    }

    private Member createMember() {
        String subject = "selective-notification-" + UUID.randomUUID();
        Member member = socialAccountService.getOrCreate(
                SocialProvider.KAKAO,
                subject,
                subject + "@example.com",
                ProviderAuthorization.none()
        ).member();
        testMemberIds.add(member.getId());
        return member;
    }
}
