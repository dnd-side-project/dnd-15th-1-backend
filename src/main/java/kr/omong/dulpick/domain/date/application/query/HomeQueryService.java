package kr.omong.dulpick.domain.date.application.query;

import kr.omong.dulpick.domain.couple.application.query.reader.CoupleConnectionReader;
import kr.omong.dulpick.domain.couple.application.query.view.CoupleConnectionStatus;
import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMemberRepository;
import kr.omong.dulpick.domain.date.application.query.view.DateCourseSummaryView;
import kr.omong.dulpick.domain.date.application.query.view.HomeOverviewView;
import kr.omong.dulpick.domain.member.application.exception.MemberNotFoundException;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.member.domain.exception.MemberNotActiveException;
import kr.omong.dulpick.domain.place.application.MemberPlaceView;
import kr.omong.dulpick.domain.place.application.PlaceQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HomeQueryService {

    private static final int MAX_RECENT_PLACE_SIZE = 50;

    private final MemberRepository memberRepository;
    private final CoupleConnectionReader coupleConnectionReader;
    private final ActiveCoupleMemberRepository activeCoupleMemberRepository;
    private final DateCourseQueryService dateCourseQueryService;
    private final PlaceQueryService placeQueryService;

    public HomeQueryService(
            MemberRepository memberRepository,
            CoupleConnectionReader coupleConnectionReader,
            ActiveCoupleMemberRepository activeCoupleMemberRepository,
            DateCourseQueryService dateCourseQueryService,
            PlaceQueryService placeQueryService
    ) {
        this.memberRepository = memberRepository;
        this.coupleConnectionReader = coupleConnectionReader;
        this.activeCoupleMemberRepository = activeCoupleMemberRepository;
        this.dateCourseQueryService = dateCourseQueryService;
        this.placeQueryService = placeQueryService;
    }

    @Transactional(readOnly = true)
    public HomeOverviewView getOverview(Long memberId) {
        validateActiveMember(memberId);
        CoupleConnectionStatus status = coupleConnectionReader.read(memberId);
        if (!status.connected()) {
            return new HomeOverviewView(false, status.me().nickname(), null, null);
        }
        Long coupleId = activeCoupleMemberRepository.findByMemberId(memberId)
                .map(membership -> membership.getCouple().getId())
                .orElse(null);
        DateCourseSummaryView currentDateCourse = coupleId == null
                ? null
                : dateCourseQueryService.findCurrentUpcomingConfirmedByCoupleId(coupleId);
        return new HomeOverviewView(
                true,
                status.me().nickname(),
                status.partner() == null ? null : status.partner().nickname(),
                currentDateCourse
        );
    }

    @Transactional(readOnly = true)
    public List<MemberPlaceView> getRecentSavedPlaces(Long memberId, int size) {
        validateActiveMember(memberId);
        int boundedSize = Math.max(1, Math.min(size, MAX_RECENT_PLACE_SIZE));
        return placeQueryService.getVisiblePlaces(memberId)
                .stream()
                .limit(boundedSize)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MemberPlaceView> getRecentSavedPlacesAll(Long memberId) {
        validateActiveMember(memberId);
        return placeQueryService.getVisiblePlaces(memberId);
    }

    @Transactional(readOnly = true)
    public List<DateCourseSummaryView> getPastDates(Long memberId, int size) {
        validateActiveMember(memberId);
        return activeCoupleMemberRepository.findByMemberId(memberId)
                .map(membership -> dateCourseQueryService.findPastConfirmedByCoupleId(
                        membership.getCouple().getId(),
                        size
                ))
                .orElse(List.of());
    }

    private void validateActiveMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        if (!member.isActive()) {
            throw new MemberNotActiveException();
        }
    }
}
