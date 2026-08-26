package kr.omong.dulpick.domain.place.application;

import jakarta.persistence.EntityManager;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.place.application.exception.InvalidPlaceCandidateException;
import kr.omong.dulpick.domain.place.application.exception.PlaceAlreadySavedException;
import kr.omong.dulpick.domain.place.domain.ContentRepository;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.domain.MemberPlace;
import kr.omong.dulpick.domain.place.domain.MemberPlaceRepository;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceCandidate;
import kr.omong.dulpick.domain.place.domain.PlaceCandidateRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImport;
import kr.omong.dulpick.domain.place.domain.PlaceImportRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImportStatus;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.domain.place.domain.PlaceVerificationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class PlaceStorageIntegrationTest {

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
    private ContentRepository contentRepository;

    @Autowired
    private PlaceImportReservationService reservationService;

    @Autowired
    private PlaceImportResultWriter resultWriter;

    @Autowired
    private PlaceCommandService commandService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void storesNaverMapCandidateWithoutCreatingPublicContent() {
        Member member = memberRepository.save(Member.create(NOW));
        PlaceImport placeImport = saveReviewableImport(member.getId(), ContentSourceType.NAVER_MAP);
        String claimToken = reservationService.claimPending(
                placeImport.getId(),
                NOW,
                NOW.minusSeconds(600)
        );
        long contentCount = contentRepository.count();
        VerifiedPlace verifiedPlace = verifiedPlace("FD6", "음식점 > 한식 > 육류,고기");
        ContentMetadata metadata = metadata(
                placeImport.getCanonicalUrl(),
                ContentSourceType.NAVER_MAP
        );

        resultWriter.saveSuccess(
                placeImport.getId(),
                claimToken,
                metadata,
                List.of(new VerifiedCandidate(
                        extractedPlace(),
                        verifiedPlace,
                        PlaceVerificationStatus.VERIFIED
                )),
                false
        );
        entityManager.flush();
        entityManager.clear();

        PlaceImport storedImport = importRepository.findById(placeImport.getId()).orElseThrow();
        Place storedPlace = placeRepository
                .findByKakaoPlaceId(verifiedPlace.kakaoPlaceId())
                .orElseThrow();
        assertThat(storedImport.getStatus()).isEqualTo(PlaceImportStatus.REVIEW_REQUIRED);
        assertThat(storedImport.getContentId()).isNull();
        assertThat(contentRepository.count()).isEqualTo(contentCount);
        assertThat(candidateRepository.findAllByImportIdOrderByIdAsc(placeImport.getId()))
                .hasSize(1);
        assertThat(storedPlace.getCategory()).isEqualTo("음식점 > 한식 > 육류,고기");
        assertThat(storedPlace.getCategoryGroupCode()).isEqualTo("FD6");
        assertThat(storedPlace.getCategoryName()).isEqualTo("맛집");
    }

    @Test
    void rejectsAllSelectionsBeforeWriteWhenOnePlaceIsAlreadySaved() {
        Member member = memberRepository.save(Member.create(NOW));
        PlaceImport placeImport = saveReviewableImport(
                member.getId(),
                ContentSourceType.INSTAGRAM_REEL
        );
        Place first = placeRepository.save(place("first"));
        Place duplicate = placeRepository.save(place("duplicate"));
        PlaceCandidate firstCandidate = candidateRepository.save(candidate(placeImport, first));
        PlaceCandidate duplicateCandidate = candidateRepository.save(candidate(placeImport, duplicate));
        memberPlaceRepository.save(MemberPlace.save(
                member.getId(),
                duplicate,
                null,
                null,
                NOW
        ));
        entityManager.flush();

        assertThatThrownBy(() -> commandService.confirm(
                member.getId(),
                placeImport.getId(),
                List.of(
                        new PlaceCommandService.PlaceSelection(firstCandidate.getId(), null),
                        new PlaceCommandService.PlaceSelection(duplicateCandidate.getId(), null)
                )
        )).isInstanceOf(PlaceAlreadySavedException.class);

        assertThat(memberPlaceRepository.findAllByMemberIdOrderBySavedAtDesc(member.getId()))
                .extracting(saved -> saved.getPlace().getId())
                .containsExactly(duplicate.getId());
    }

    @Test
    void rejectsCandidateOwnedByDifferentImport() {
        Member member = memberRepository.save(Member.create(NOW));
        PlaceImport requestedImport = saveReviewableImport(
                member.getId(),
                ContentSourceType.INSTAGRAM_REEL
        );
        PlaceImport otherImport = saveReviewableImport(
                member.getId(),
                ContentSourceType.NAVER_BLOG
        );
        PlaceCandidate otherCandidate = candidateRepository.save(candidate(
                otherImport,
                placeRepository.save(place("other"))
        ));
        entityManager.flush();

        assertThatThrownBy(() -> commandService.confirm(
                member.getId(),
                requestedImport.getId(),
                List.of(new PlaceCommandService.PlaceSelection(otherCandidate.getId(), null))
        )).isInstanceOf(InvalidPlaceCandidateException.class);

        assertThat(memberPlaceRepository.findAllByMemberIdOrderBySavedAtDesc(member.getId()))
                .isEmpty();
    }

    private PlaceImport saveReviewableImport(Long memberId, ContentSourceType sourceType) {
        String uniqueKey = UUID.randomUUID().toString();
        PlaceImport placeImport = PlaceImport.receive(
                memberId,
                "https://example.test/" + uniqueKey,
                uniqueKey,
                sourceType,
                NOW
        );
        if (sourceType != ContentSourceType.NAVER_MAP) {
            placeImport.complete("title", "content", null, "hash", NOW, NOW);
        }
        return importRepository.saveAndFlush(placeImport);
    }

    private Place place(String suffix) {
        return Place.create(
                "kakao-" + suffix + "-" + UUID.randomUUID(),
                "테스트 장소 " + suffix,
                "서울 성동구 성수동",
                "서울 성동구 연무장길 1",
                new BigDecimal("37.5445000"),
                new BigDecimal("127.0560000"),
                "음식점 > 카페",
                "CE7",
                null,
                NOW
        );
    }

    private PlaceCandidate candidate(PlaceImport placeImport, Place place) {
        return PlaceCandidate.verified(
                placeImport.getId(),
                place.getId(),
                place.getName(),
                place.getRoadAddress(),
                place.getName(),
                "EXPLICIT_VENUE",
                NOW
        );
    }

    private ExtractedPlace extractedPlace() {
        return new ExtractedPlace(
                "을지식당",
                "서울 중구 을지로40길 17",
                "을지식당 서울 중구 을지로40길 17",
                "EXPLICIT_VENUE"
        );
    }

    private VerifiedPlace verifiedPlace(String categoryGroupCode, String category) {
        return new VerifiedPlace(
                "kakao-naver-" + UUID.randomUUID(),
                "을지식당",
                "서울 중구 을지로6가 67-3",
                "서울 중구 을지로40길 17",
                new BigDecimal("37.5659000"),
                new BigDecimal("127.0044624"),
                categoryGroupCode,
                category,
                null
        );
    }

    private ContentMetadata metadata(String canonicalUrl, ContentSourceType sourceType) {
        return new ContentMetadata(
                canonicalUrl,
                sourceType,
                "을지식당",
                "서울 중구 을지로40길 17",
                null,
                "naver-content-hash",
                NOW,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
