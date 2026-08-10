package kr.omong.dulpick.domain.place.presentation.dto.response;

import kr.omong.dulpick.domain.place.application.PlaceCandidateView;
import kr.omong.dulpick.domain.place.application.PlaceImportNextAction;
import kr.omong.dulpick.domain.place.application.PlaceImportView;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.domain.PlaceImportStatus;
import kr.omong.dulpick.domain.place.domain.PlaceVerificationStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceImportResponseTest {

    @Test
    void mapsStructuredContentAndVerifiedCandidate() {
        Instant checkedAt = Instant.parse("2026-08-09T06:30:00Z");
        PlaceCandidateView candidate = new PlaceCandidateView(
                100L,
                PlaceVerificationStatus.VERIFIED,
                "밀빛",
                "서울 마포구",
                new PlaceCandidateView.VerifiedPlaceView(
                        200L,
                        "kakao-id",
                        "밀빛 망원점",
                        "서울 마포구 망원동",
                        "서울 마포구 희우정로",
                        new BigDecimal("37.5546637"),
                        new BigDecimal("126.9033951"),
                        "음식점 > 제과,베이커리",
                        "맛집",
                        true,
                        null
                ),
                "망원 카페 - 밀빛",
                "EXPLICIT_VENUE"
        );
        PlaceImportView view = new PlaceImportView(
                1L,
                10L,
                "https://www.instagram.com/reel/example",
                ContentSourceType.INSTAGRAM_REEL,
                PlaceImportStatus.REVIEW_REQUIRED,
                PlaceImportNextAction.SELECT_PLACES,
                null,
                null,
                new PlaceImportView.ContentView(
                        "망원동 빵지순례",
                        "망원동에 새로 생긴 베이커리",
                        "https://example.com/thumbnail.jpg",
                        new PlaceImportView.AuthorView("찐", "jjin_.record"),
                        LocalDate.of(2026, 8, 7),
                        new PlaceImportView.EngagementView(1_833L, 76L, checkedAt)
                ),
                checkedAt,
                checkedAt,
                List.of(candidate)
        );

        PlaceImportResponse response = PlaceImportResponse.from(view);

        assertThat(response.canonicalUrl()).isEqualTo(view.canonicalUrl());
        assertThat(response.content().author().username()).isEqualTo("jjin_.record");
        assertThat(response.content().engagement().likeCount()).isEqualTo(1_833L);
        assertThat(response.candidates()).singleElement().satisfies(result -> {
            assertThat(result.verificationStatus()).isEqualTo(PlaceVerificationStatus.VERIFIED);
            assertThat(result.place().placeId()).isEqualTo(200L);
            assertThat(result.place().categoryName()).isEqualTo("맛집");
            assertThat(result.place().savedByMe()).isTrue();
        });
    }
}
