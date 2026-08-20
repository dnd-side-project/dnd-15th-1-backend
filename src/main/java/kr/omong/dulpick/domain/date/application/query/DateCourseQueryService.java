package kr.omong.dulpick.domain.date.application.query;

import kr.omong.dulpick.domain.couple.application.exception.CoupleNotFoundException;
import kr.omong.dulpick.domain.couple.application.exception.CoupleStateInvalidException;
import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMember;
import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMemberRepository;
import kr.omong.dulpick.domain.date.application.exception.DateCourseNotFoundException;
import kr.omong.dulpick.domain.date.application.query.view.DateCourseCategoryOptionView;
import kr.omong.dulpick.domain.date.application.query.view.DateCoursePlaceCandidateView;
import kr.omong.dulpick.domain.date.application.query.view.DateCoursePlacePoolView;
import kr.omong.dulpick.domain.date.application.query.view.DateCoursePlaceView;
import kr.omong.dulpick.domain.date.application.query.view.DateCourseSummaryView;
import kr.omong.dulpick.domain.date.application.query.view.DateCourseView;
import kr.omong.dulpick.domain.date.application.support.PlaceRegionExtractor;
import kr.omong.dulpick.domain.date.domain.DateCourse;
import kr.omong.dulpick.domain.date.domain.DateCoursePlace;
import kr.omong.dulpick.domain.date.domain.DateCoursePlaceRepository;
import kr.omong.dulpick.domain.date.domain.DateCourseRepository;
import kr.omong.dulpick.domain.date.domain.DateCourseStatus;
import kr.omong.dulpick.domain.member.application.exception.MemberNotFoundException;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.member.domain.exception.MemberNotActiveException;
import kr.omong.dulpick.domain.place.application.MemberPlaceView;
import kr.omong.dulpick.domain.place.application.PlaceQueryService;
import kr.omong.dulpick.domain.place.application.PlaceWalkingRouteService;
import kr.omong.dulpick.domain.place.application.WalkingRoute;
import kr.omong.dulpick.domain.place.domain.DulpickPlaceCategory;
import kr.omong.dulpick.domain.place.domain.Place;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class DateCourseQueryService {

    private static final int DEFAULT_MAX_SIZE = 50;

    private final MemberRepository memberRepository;
    private final ActiveCoupleMemberRepository activeCoupleMemberRepository;
    private final DateCourseRepository dateCourseRepository;
    private final DateCoursePlaceRepository dateCoursePlaceRepository;
    private final PlaceQueryService placeQueryService;
    private final PlaceWalkingRouteService placeWalkingRouteService;
    private final PlaceRegionExtractor placeRegionExtractor;
    private final Clock clock;

    public DateCourseQueryService(
            MemberRepository memberRepository,
            ActiveCoupleMemberRepository activeCoupleMemberRepository,
            DateCourseRepository dateCourseRepository,
            DateCoursePlaceRepository dateCoursePlaceRepository,
            PlaceQueryService placeQueryService,
            PlaceWalkingRouteService placeWalkingRouteService,
            PlaceRegionExtractor placeRegionExtractor,
            Clock clock
    ) {
        this.memberRepository = memberRepository;
        this.activeCoupleMemberRepository = activeCoupleMemberRepository;
        this.dateCourseRepository = dateCourseRepository;
        this.dateCoursePlaceRepository = dateCoursePlaceRepository;
        this.placeQueryService = placeQueryService;
        this.placeWalkingRouteService = placeWalkingRouteService;
        this.placeRegionExtractor = placeRegionExtractor;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DateCoursePlacePoolView getCoupleSavedPlacePool(
            Long memberId,
            String region,
            DulpickPlaceCategory category
    ) {
        requireCoupleContext(memberId);
        List<DateCoursePlaceCandidateView> allCandidates = placeQueryService.getVisiblePlaces(memberId)
                .stream()
                .map(this::toCandidateView)
                .toList();
        List<String> availableRegions = allCandidates.stream()
                .map(DateCoursePlaceCandidateView::region)
                .distinct()
                .sorted()
                .toList();
        Set<String> categoryNames = allCandidates.stream()
                .map(DateCoursePlaceCandidateView::categoryName)
                .collect(Collectors.toSet());
        List<DateCourseCategoryOptionView> availableCategories = List.of(DulpickPlaceCategory.values())
                .stream()
                .filter(value -> categoryNames.contains(value.getDisplayName()))
                .map(value -> new DateCourseCategoryOptionView(value, value.getDisplayName()))
                .toList();
        List<DateCoursePlaceCandidateView> filtered = allCandidates.stream()
                .filter(candidate -> placeRegionExtractor.matchesRegionFilter(
                        candidate.region(),
                        region
                ))
                .filter(candidate -> category == null
                        || category.getDisplayName().equals(candidate.categoryName()))
                .toList();
        return new DateCoursePlacePoolView(filtered, availableRegions, availableCategories);
    }

    @Transactional
    public DateCourseView getDateCourse(Long memberId, Long dateCourseId) {
        CoupleContext context = requireCoupleContext(memberId);
        DateCourse dateCourse = dateCourseRepository.findByIdAndCoupleId(dateCourseId, context.coupleId())
                .orElseThrow(DateCourseNotFoundException::new);
        List<DateCoursePlace> places = dateCoursePlaceRepository
                .findAllByDateCourseIdOrderBySequenceOrderAsc(dateCourseId);
        return toView(dateCourse, places);
    }

    @Transactional(readOnly = true)
    public DateCourseSummaryView getCurrentUpcomingConfirmed(Long memberId) {
        CoupleContext context = requireCoupleContext(memberId);
        return findCurrentUpcomingConfirmedByCoupleId(context.coupleId());
    }

    @Transactional(readOnly = true)
    public List<DateCourseSummaryView> getPastConfirmed(Long memberId, int size) {
        CoupleContext context = requireCoupleContext(memberId);
        return findPastConfirmedByCoupleId(context.coupleId(), size);
    }

    @Transactional(readOnly = true)
    public DateCourseSummaryView findCurrentUpcomingConfirmedByCoupleId(Long coupleId) {
        List<DateCourse> courses = dateCourseRepository.findUpcoming(
                coupleId,
                DateCourseStatus.CONFIRMED,
                clock.instant(),
                PageRequest.of(0, 1)
        );
        if (courses.isEmpty()) {
            return null;
        }
        return toSummary(courses.getFirst());
    }

    @Transactional(readOnly = true)
    public List<DateCourseSummaryView> findPastConfirmedByCoupleId(Long coupleId, int size) {
        int boundedSize = Math.max(1, Math.min(size, DEFAULT_MAX_SIZE));
        return dateCourseRepository.findPast(
                        coupleId,
                        DateCourseStatus.CONFIRMED,
                        clock.instant(),
                        PageRequest.of(0, boundedSize)
                )
                .stream()
                .map(this::toSummary)
                .toList();
    }

    private DateCoursePlaceCandidateView toCandidateView(MemberPlaceView placeView) {
        return new DateCoursePlaceCandidateView(
                placeView.placeId(),
                placeView.name(),
                placeView.address(),
                placeView.roadAddress(),
                placeRegionExtractor.extract(placeView.roadAddress(), placeView.address()),
                placeView.latitude(),
                placeView.longitude(),
                placeView.category(),
                placeView.categoryName(),
                placeView.ownershipStatus(),
                placeView.alias(),
                placeView.savedAt(),
                placeView.thumbnailUrl(),
                placeView.imageUrls()
        );
    }

    private DateCourseView toView(DateCourse dateCourse, List<DateCoursePlace> places) {
        List<DateCoursePlace> orderedPlaces = places.stream()
                .sorted(Comparator.comparing(DateCoursePlace::getSequenceOrder))
                .toList();
        List<WalkingRoute> walks = placeWalkingRouteService.consecutiveWalks(
                orderedPlaces.stream().map(DateCoursePlace::getPlace).toList()
        );
        List<DateCoursePlaceView> placeViews = IntStream.range(0, orderedPlaces.size())
                .mapToObj(index -> toPlaceView(orderedPlaces.get(index), walks.get(index)))
                .toList();
        return new DateCourseView(
                dateCourse.getId(),
                dateCourse.getTitle(),
                dateCourse.getScheduledAt(),
                dateCourse.getStatus(),
                dateCourse.getVersion(),
                placeViews.size(),
                placeViews
        );
    }

    private DateCoursePlaceView toPlaceView(DateCoursePlace place, WalkingRoute walkToNext) {
        Place selected = place.getPlace();
        return new DateCoursePlaceView(
                place.getSequenceOrder(),
                selected.getId(),
                selected.getName(),
                selected.getAddress(),
                selected.getRoadAddress(),
                selected.getLatitude(),
                selected.getLongitude(),
                selected.getCategory(),
                selected.getCategoryName(),
                selected.getThumbnailUrl(),
                selected.getImageUrls(),
                walkToNext
        );
    }

    private DateCourseSummaryView toSummary(DateCourse dateCourse) {
        return new DateCourseSummaryView(
                dateCourse.getId(),
                dateCourse.getTitle(),
                dateCourse.getScheduledAt(),
                dateCourse.getStatus(),
                dateCourse.getVersion(),
                (int) dateCoursePlaceRepository.countByDateCourseId(dateCourse.getId())
        );
    }

    private CoupleContext requireCoupleContext(Long memberId) {
        validateActiveMember(memberId);
        ActiveCoupleMember membership = activeCoupleMemberRepository.findByMemberId(memberId)
                .orElseThrow(CoupleNotFoundException::new);
        List<Long> memberIds = activeCoupleMemberRepository
                .findAllByCoupleId(membership.getCouple().getId())
                .stream()
                .map(ActiveCoupleMember::getMemberId)
                .sorted()
                .toList();
        if (memberIds.size() != 2 || !memberIds.contains(memberId)) {
            throw new CoupleStateInvalidException();
        }
        return new CoupleContext(membership.getCouple().getId(), memberIds);
    }

    private void validateActiveMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        if (!member.isActive()) {
            throw new MemberNotActiveException();
        }
    }

    private record CoupleContext(Long coupleId, List<Long> memberIds) {
    }
}
