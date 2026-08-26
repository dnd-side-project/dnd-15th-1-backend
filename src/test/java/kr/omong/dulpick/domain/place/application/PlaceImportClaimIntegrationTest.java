package kr.omong.dulpick.domain.place.application;

import jakarta.persistence.EntityManager;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.domain.PlaceImport;
import kr.omong.dulpick.domain.place.domain.PlaceImportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PlaceImportClaimIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-08-10T03:00:00Z");

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PlaceImportRepository importRepository;

    @Autowired
    private PlaceImportReservationService reservationService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void replacesStaleClaimAndRejectsPreviousWorkerToken() {
        PlaceImport placeImport = saveImport();
        String firstClaim = reservationService.claimPending(
                placeImport.getId(),
                NOW,
                NOW.minusSeconds(600)
        );
        entityManager.clear();

        String secondClaim = reservationService.claimPending(
                placeImport.getId(),
                NOW.plusSeconds(601),
                NOW.plusSeconds(1)
        );
        entityManager.clear();

        assertThat(firstClaim).isNotNull();
        assertThat(secondClaim).isNotNull().isNotEqualTo(firstClaim);
        assertThat(importRepository.findClaimedForUpdate(placeImport.getId(), firstClaim))
                .isEmpty();
        assertThat(importRepository.findClaimedForUpdate(placeImport.getId(), secondClaim))
                .isPresent();
    }

    @Test
    void requeuesFailedImportOnlyAfterRetryCooldown() {
        PlaceImport placeImport = saveImport();
        placeImport.fail("PLACE_VERIFICATION_UNAVAILABLE", NOW);
        importRepository.saveAndFlush(placeImport);
        entityManager.clear();

        boolean beforeCooldown = reservationService.requeueRetryable(
                placeImport.getId(),
                NOW.plusSeconds(299),
                NOW.minusSeconds(600),
                NOW.minusSeconds(1)
        );
        boolean afterCooldown = reservationService.requeueRetryable(
                placeImport.getId(),
                NOW.plusSeconds(300),
                NOW.minusSeconds(600),
                NOW
        );

        assertThat(beforeCooldown).isFalse();
        assertThat(afterCooldown).isTrue();
    }

    private PlaceImport saveImport() {
        Member member = memberRepository.save(Member.create(NOW));
        String uniqueKey = UUID.randomUUID().toString();
        return importRepository.saveAndFlush(PlaceImport.receive(
                member.getId(),
                "https://www.instagram.com/reel/" + uniqueKey,
                uniqueKey,
                ContentSourceType.INSTAGRAM_REEL,
                NOW
        ));
    }
}
