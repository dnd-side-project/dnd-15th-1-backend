package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.domain.MemberPlaceRepository;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceCandidate;
import kr.omong.dulpick.domain.place.domain.PlaceCandidateRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImport;
import kr.omong.dulpick.domain.place.domain.PlaceImportRepository;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PlaceConfirmationConcurrencyIntegrationTest {

    private static final int CONCURRENT_REQUESTS = 2;
    private static final Instant NOW = Instant.parse("2026-08-10T03:00:00Z");

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PlaceImportRepository importRepository;

    @Autowired
    private PlaceCandidateRepository candidateRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private MemberPlaceRepository memberPlaceRepository;

    @Autowired
    private PlaceCommandService commandService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Long memberId;
    private Long importId;
    private Long placeId;

    @AfterEach
    void cleanUp() {
        if (memberId == null) {
            return;
        }
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> cleanUpData());
    }

    private void cleanUpData() {
        memberPlaceRepository.deleteAll(
                memberPlaceRepository.findAllByMemberIdOrderBySavedAtDesc(memberId)
        );
        memberPlaceRepository.flush();
        if (importId != null) {
            candidateRepository.deleteAllByImportId(importId);
            candidateRepository.flush();
            importRepository.deleteById(importId);
            importRepository.flush();
        }
        if (placeId != null) {
            placeRepository.deleteById(placeId);
            placeRepository.flush();
        }
        memberRepository.deleteById(memberId);
    }

    @Test
    @Timeout(10)
    void allowsOnlyOneConcurrentConfirmationForSamePlace() throws Exception {
        PlaceCandidate candidate = setUpCandidate();
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CountDownLatch ready = new CountDownLatch(CONCURRENT_REQUESTS);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<Object>> futures = List.of(
                    submitConfirmation(executor, ready, start, candidate.getId()),
                    submitConfirmation(executor, ready, start, candidate.getId())
            );
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<PlaceConfirmationView> results = futures.stream()
                    .map(this::getResult)
                    .map(PlaceConfirmationView.class::cast)
                    .toList();

            assertThat(results).hasSize(CONCURRENT_REQUESTS);
            assertThat(results.stream()
                    .map(result -> result.savedPlaces().get(0).newlySaved())
                    .toList())
                    .containsExactlyInAnyOrder(true, false);
            assertThat(memberPlaceRepository.findAllByMemberIdOrderBySavedAtDesc(memberId))
                    .hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private PlaceCandidate setUpCandidate() {
        Member member = memberRepository.save(Member.create(NOW));
        memberId = member.getId();
        String uniqueKey = UUID.randomUUID().toString();
        PlaceImport placeImport = PlaceImport.receive(
                memberId,
                "https://www.instagram.com/reel/" + uniqueKey,
                uniqueKey,
                ContentSourceType.INSTAGRAM_REEL,
                NOW
        );
        placeImport.complete("title", "content", null, "hash", NOW, NOW);
        placeImport = importRepository.save(placeImport);
        importId = placeImport.getId();
        Place place = placeRepository.save(Place.create(
                "kakao-" + uniqueKey,
                "동시 저장 장소",
                "서울 성동구 성수동",
                "서울 성동구 연무장길 1",
                new BigDecimal("37.5445000"),
                new BigDecimal("127.0560000"),
                "음식점 > 카페",
                "CE7",
                null,
                NOW
        ));
        placeId = place.getId();
        return candidateRepository.save(PlaceCandidate.verified(
                importId,
                placeId,
                place.getName(),
                place.getRoadAddress(),
                place.getName(),
                "EXPLICIT_VENUE",
                NOW
        ));
    }

    private Future<Object> submitConfirmation(
            ExecutorService executor,
            CountDownLatch ready,
            CountDownLatch start,
            Long candidateId
    ) {
        return executor.submit(() -> {
            ready.countDown();
            start.await();
            return commandService.confirm(
                    memberId,
                    importId,
                    List.of(new PlaceCommandService.PlaceSelection(candidateId, null))
            );
        });
    }

    private PlaceConfirmationView getResult(Future<Object> future) {
        try {
            return (PlaceConfirmationView) future.get(5, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
