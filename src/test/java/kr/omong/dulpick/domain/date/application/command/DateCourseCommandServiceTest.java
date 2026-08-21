package kr.omong.dulpick.domain.date.application.command;

import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMember;
import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMemberRepository;
import kr.omong.dulpick.domain.couple.domain.Couple;
import kr.omong.dulpick.domain.date.application.exception.DateCourseConflictException;
import kr.omong.dulpick.domain.date.application.exception.DateCoursePlaceNotSavedException;
import kr.omong.dulpick.domain.date.application.exception.DateCoursePlaceRequiredException;
import kr.omong.dulpick.domain.date.domain.DateCourse;
import kr.omong.dulpick.domain.date.domain.DateCoursePlaceRepository;
import kr.omong.dulpick.domain.date.domain.DateCourseRepository;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberProfileRepository;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.place.application.PlaceWalkingRouteService;
import kr.omong.dulpick.domain.place.domain.MemberPlaceRepository;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DateCourseCommandServiceTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final MemberProfileRepository memberProfileRepository = mock(MemberProfileRepository.class);
    private final ActiveCoupleMemberRepository activeCoupleMemberRepository =
            mock(ActiveCoupleMemberRepository.class);
    private final DateCourseRepository dateCourseRepository = mock(DateCourseRepository.class);
    private final DateCoursePlaceRepository dateCoursePlaceRepository =
            mock(DateCoursePlaceRepository.class);
    private final MemberPlaceRepository memberPlaceRepository = mock(MemberPlaceRepository.class);
    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceWalkingRouteService placeWalkingRouteService = mock(PlaceWalkingRouteService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final DateCourseCommandService service = new DateCourseCommandService(
            memberRepository,
            memberProfileRepository,
            activeCoupleMemberRepository,
            dateCourseRepository,
            dateCoursePlaceRepository,
            memberPlaceRepository,
            placeRepository,
            placeWalkingRouteService,
            eventPublisher,
            Clock.fixed(Instant.parse("2026-08-16T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void requiresAtLeastOnePlaceWhenSavingDateCourse() {
        preparedCourse(0L);

        assertThatThrownBy(() -> service.save(
                1L,
                10L,
                command(0L, List.of())
        )).isInstanceOf(DateCoursePlaceRequiredException.class);
    }

    @Test
    void rejectsPlacesOutsideCoupleSavedPool() {
        preparedCourse(0L);
        when(memberPlaceRepository.findAllByMemberIdInOrderBySavedAtDesc(List.of(1L, 2L)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.save(
                1L,
                10L,
                command(0L, List.of(100L))
        )).isInstanceOf(DateCoursePlaceNotSavedException.class);
    }

    @Test
    void rejectsStaleVersionBeforeSaving() {
        preparedCourse(2L);

        assertThatThrownBy(() -> service.save(
                1L,
                10L,
                command(1L, List.of())
        )).isInstanceOf(DateCourseConflictException.class);
    }

    private DateCourse preparedCourse(long version) {
        Member member = mock(Member.class);
        when(member.isActive()).thenReturn(true);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        Couple couple = mock(Couple.class);
        when(couple.getId()).thenReturn(99L);
        ActiveCoupleMember mine = mock(ActiveCoupleMember.class);
        ActiveCoupleMember partner = mock(ActiveCoupleMember.class);
        when(mine.getCouple()).thenReturn(couple);
        when(mine.getMemberId()).thenReturn(1L);
        when(partner.getMemberId()).thenReturn(2L);
        when(activeCoupleMemberRepository.findByMemberId(1L)).thenReturn(Optional.of(mine));
        when(activeCoupleMemberRepository.findAllByCoupleId(99L)).thenReturn(List.of(mine, partner));

        DateCourse dateCourse = mock(DateCourse.class);
        when(dateCourse.getVersion()).thenReturn(version);
        when(dateCourseRepository.findByIdAndCoupleId(10L, 99L)).thenReturn(Optional.of(dateCourse));
        return dateCourse;
    }

    private SaveDateCourseCommand command(
            long version,
            List<Long> placeIds
    ) {
        return new SaveDateCourseCommand(
                version,
                "데이트",
                LocalDate.of(2026, 8, 20),
                LocalTime.of(19, 30),
                placeIds
        );
    }
}
