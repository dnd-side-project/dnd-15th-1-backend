package kr.omong.dulpick.domain.place;

import kr.omong.dulpick.domain.place.domain.PlaceCandidate;
import kr.omong.dulpick.domain.place.domain.PlaceVerificationStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceCandidateTest {

    @Test
    void operatorVerificationConnectsCandidateToTheSelectedPlace() {
        PlaceCandidate candidate = PlaceCandidate.extracted(
                10L,
                "성수 카페",
                "성동구",
                "운영자 검토 대상",
                "PLACE",
                Instant.parse("2026-08-25T00:00:00Z")
        );

        candidate.adminVerify(20L);

        assertThat(candidate.getPlaceId()).isEqualTo(20L);
        assertThat(candidate.getVerificationStatus()).isEqualTo(PlaceVerificationStatus.VERIFIED);
    }
}
