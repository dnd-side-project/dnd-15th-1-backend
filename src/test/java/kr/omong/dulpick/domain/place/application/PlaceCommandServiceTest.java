package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMemberRepository;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.place.application.exception.PlaceAlreadySavedException;
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
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlaceCommandServiceTest {

    @Test
    void returnsCompletedStatusAndWhetherPlaceWasNewlySaved() {
        Instant now = Instant.parse("2026-08-10T00:00:00Z");
        PlaceImportRepository importRepository = mock(PlaceImportRepository.class);
        PlaceCandidateRepository candidateRepository = mock(PlaceCandidateRepository.class);
        PlaceRepository placeRepository = mock(PlaceRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        MemberPlaceRepository memberPlaceRepository = mock(MemberPlaceRepository.class);
        ActiveCoupleMemberRepository coupleRepository = mock(ActiveCoupleMemberRepository.class);
        PlaceCommandService service = new PlaceCommandService(
                importRepository,
                candidateRepository,
                placeRepository,
                memberRepository,
                memberPlaceRepository,
                coupleRepository,
                mock(ApplicationEventPublisher.class),
                Clock.fixed(now, ZoneOffset.UTC)
        );
        PlaceImport placeImport = mock(PlaceImport.class);
        PlaceCandidate candidate = mock(PlaceCandidate.class);
        Place place = mock(Place.class);
        Member member = mock(Member.class);
        when(importRepository.findById(10L)).thenReturn(Optional.of(placeImport));
        when(placeImport.getMemberId()).thenReturn(1L);
        when(placeImport.getStatus())
                .thenReturn(PlaceImportStatus.REVIEW_REQUIRED)
                .thenReturn(PlaceImportStatus.COMPLETED);
        when(memberRepository.findForUpdateById(1L)).thenReturn(Optional.of(member));
        when(member.isActive()).thenReturn(true);
        when(candidateRepository.findAllById(List.of(100L))).thenReturn(List.of(candidate));
        when(candidate.getId()).thenReturn(100L);
        when(candidate.getImportId()).thenReturn(10L);
        when(candidate.getVerificationStatus()).thenReturn(PlaceVerificationStatus.VERIFIED);
        when(candidate.getPlaceId()).thenReturn(20L);
        when(placeRepository.findById(20L)).thenReturn(Optional.of(place));
        when(place.getId()).thenReturn(20L);
        when(place.getName()).thenReturn("밀빛 망원점");
        when(memberPlaceRepository.findByMemberIdAndPlaceId(1L, 20L))
                .thenReturn(Optional.empty());
        when(memberPlaceRepository.save(any(MemberPlace.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(coupleRepository.findByMemberId(1L)).thenReturn(Optional.empty());

        PlaceConfirmationView result = service.confirm(
                1L,
                10L,
                List.of(new PlaceCommandService.PlaceSelection(100L, null, null))
        );

        assertThat(result.importId()).isEqualTo(10L);
        assertThat(result.status()).isEqualTo(PlaceImportStatus.COMPLETED);
        assertThat(result.savedPlaces()).singleElement().satisfies(saved -> {
            assertThat(saved.newlySaved()).isTrue();
            assertThat(saved.place().placeId()).isEqualTo(20L);
        });
        verify(placeImport).markCompleted(now);
    }

    @Test
    void reportsAlreadySavedWhenCompletedImportIsConfirmedAgain() {
        Instant now = Instant.parse("2026-08-10T00:00:00Z");
        PlaceImportRepository importRepository = mock(PlaceImportRepository.class);
        PlaceCandidateRepository candidateRepository = mock(PlaceCandidateRepository.class);
        PlaceRepository placeRepository = mock(PlaceRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        MemberPlaceRepository memberPlaceRepository = mock(MemberPlaceRepository.class);
        PlaceCommandService service = new PlaceCommandService(
                importRepository,
                candidateRepository,
                placeRepository,
                memberRepository,
                memberPlaceRepository,
                mock(ActiveCoupleMemberRepository.class),
                mock(ApplicationEventPublisher.class),
                Clock.fixed(now, ZoneOffset.UTC)
        );
        Member member = mock(Member.class);
        PlaceImport placeImport = mock(PlaceImport.class);
        PlaceCandidate candidate = mock(PlaceCandidate.class);
        Place place = mock(Place.class);
        MemberPlace existing = mock(MemberPlace.class);
        when(memberRepository.findForUpdateById(1L)).thenReturn(Optional.of(member));
        when(member.isActive()).thenReturn(true);
        when(importRepository.findById(10L)).thenReturn(Optional.of(placeImport));
        when(placeImport.getMemberId()).thenReturn(1L);
        when(placeImport.getStatus()).thenReturn(PlaceImportStatus.COMPLETED);
        when(candidateRepository.findAllById(List.of(100L))).thenReturn(List.of(candidate));
        when(candidate.getId()).thenReturn(100L);
        when(candidate.getImportId()).thenReturn(10L);
        when(candidate.getVerificationStatus()).thenReturn(PlaceVerificationStatus.VERIFIED);
        when(candidate.getPlaceId()).thenReturn(20L);
        when(place.getId()).thenReturn(20L);
        when(existing.getPlace()).thenReturn(place);
        when(memberPlaceRepository.findAllByMemberIdAndPlaceIdIn(1L, List.of(20L)))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.confirm(
                1L,
                10L,
                List.of(new PlaceCommandService.PlaceSelection(100L, null, null))
        )).isInstanceOf(PlaceAlreadySavedException.class);
    }
}
