package kr.omong.dulpick.domain.date.application.command;

import kr.omong.dulpick.domain.couple.application.exception.CoupleNotFoundException;
import kr.omong.dulpick.domain.couple.application.exception.CoupleStateInvalidException;
import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMember;
import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMemberRepository;
import kr.omong.dulpick.domain.date.application.exception.DateCourseConflictException;
import kr.omong.dulpick.domain.date.application.exception.DateCourseNotFoundException;
import kr.omong.dulpick.domain.date.application.exception.DateCoursePlaceNotSavedException;
import kr.omong.dulpick.domain.date.application.exception.DateCoursePlaceRequiredException;
import kr.omong.dulpick.domain.date.application.query.view.DateCoursePlaceView;
import kr.omong.dulpick.domain.date.application.query.view.DateCourseView;
import kr.omong.dulpick.domain.date.domain.DateCourse;
import kr.omong.dulpick.domain.date.domain.DateCoursePlace;
import kr.omong.dulpick.domain.date.domain.DateCoursePlaceRepository;
import kr.omong.dulpick.domain.date.domain.DateCourseRepository;
import kr.omong.dulpick.domain.date.domain.exception.InvalidDateCourseException;
import kr.omong.dulpick.domain.member.application.exception.MemberNotFoundException;
import kr.omong.dulpick.domain.member.application.exception.MemberProfileRequiredException;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberProfile;
import kr.omong.dulpick.domain.member.domain.MemberProfileRepository;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.member.domain.exception.MemberNotActiveException;
import kr.omong.dulpick.domain.notification.application.event.DateCoursePlannedEvent;
import kr.omong.dulpick.domain.place.application.PlaceWalkingRouteService;
import kr.omong.dulpick.domain.place.application.WalkingRoute;
import kr.omong.dulpick.domain.place.application.exception.PlaceNotFoundException;
import kr.omong.dulpick.domain.place.domain.MemberPlaceRepository;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.global.time.ServiceTime;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class DateCourseCommandService {

    private final MemberRepository memberRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final ActiveCoupleMemberRepository activeCoupleMemberRepository;
    private final DateCourseRepository dateCourseRepository;
    private final DateCoursePlaceRepository dateCoursePlaceRepository;
    private final MemberPlaceRepository memberPlaceRepository;
    private final PlaceRepository placeRepository;
    private final PlaceWalkingRouteService placeWalkingRouteService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public DateCourseCommandService(
            MemberRepository memberRepository,
            MemberProfileRepository memberProfileRepository,
            ActiveCoupleMemberRepository activeCoupleMemberRepository,
            DateCourseRepository dateCourseRepository,
            DateCoursePlaceRepository dateCoursePlaceRepository,
            MemberPlaceRepository memberPlaceRepository,
            PlaceRepository placeRepository,
            PlaceWalkingRouteService placeWalkingRouteService,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.memberRepository = memberRepository;
        this.memberProfileRepository = memberProfileRepository;
        this.activeCoupleMemberRepository = activeCoupleMemberRepository;
        this.dateCourseRepository = dateCourseRepository;
        this.dateCoursePlaceRepository = dateCoursePlaceRepository;
        this.memberPlaceRepository = memberPlaceRepository;
        this.placeRepository = placeRepository;
        this.placeWalkingRouteService = placeWalkingRouteService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public DateCourseView create(Long memberId, CreateDateCourseCommand command) {
        CoupleContext context = requireCoupleContext(memberId);
        Instant now = clock.instant();
        DateCourse dateCourse = dateCourseRepository.save(DateCourse.create(
                context.coupleId(),
                memberId,
                command.title(),
                toInstant(command.date(), command.time()),
                now
        ));
        return toView(dateCourse, List.of());
    }

    @Transactional
    public DateCourseView save(
            Long memberId,
            Long dateCourseId,
            SaveDateCourseCommand command
    ) {
        CoupleContext context = requireCoupleContext(memberId);
        DateCourse dateCourse = dateCourseRepository.findByIdAndCoupleId(
                dateCourseId,
                context.coupleId()
        ).orElseThrow(DateCourseNotFoundException::new);
        if (dateCourse.getVersion() != command.version()) {
            throw new DateCourseConflictException();
        }

        List<Long> placeIds = validatedPlaceIds(command.placeIds());
        if (placeIds.isEmpty()) {
            throw new DateCoursePlaceRequiredException();
        }
        validateCoupleSavedPlaces(context.memberIds(), placeIds);
        List<DateCoursePlace> newPlaces = placeIdsToCoursePlaces(dateCourseId, placeIds, clock.instant());

        Instant now = clock.instant();
        dateCourse.confirm(command.title(), toInstant(command.date(), command.time()), now);

        dateCoursePlaceRepository.deleteAllByDateCourseId(dateCourseId);
        dateCoursePlaceRepository.flush();
        if (!newPlaces.isEmpty()) {
            dateCoursePlaceRepository.saveAll(newPlaces);
        }
        try {
            dateCourseRepository.save(dateCourse);
            dateCourseRepository.flush();
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new DateCourseConflictException();
        }
        return toView(dateCourse, newPlaces);
    }

    @Transactional
    public DateCoursePartnerNotified notifyPartner(Long memberId, Long dateCourseId) {
        CoupleContext context = requireCoupleContext(memberId);
        DateCourse dateCourse = dateCourseRepository.findByIdAndCoupleId(dateCourseId, context.coupleId())
                .orElseThrow(DateCourseNotFoundException::new);
        Long partnerMemberId = context.partnerMemberId(memberId);
        String nickname = memberProfileRepository.findById(memberId)
                .map(MemberProfile::getNickname)
                .orElseThrow(MemberProfileRequiredException::new);
        Instant now = clock.instant();
        eventPublisher.publishEvent(new DateCoursePlannedEvent(
                dateCourse.getId(),
                context.coupleId(),
                memberId,
                partnerMemberId,
                nickname,
                dateCourse.getTitle(),
                now
        ));
        return new DateCoursePartnerNotified(true, partnerMemberId);
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

    private Instant toInstant(LocalDate date, LocalTime time) {
        if (date == null) {
            throw new InvalidDateCourseException(
                    "date",
                    "REQUIRED",
                    "데이트 날짜가 필요합니다"
            );
        }
        Instant scheduledAt = ServiceTime.toScheduledInstant(date, time);
        if (scheduledAt == null) {
            throw new InvalidDateCourseException(
                    "scheduledAt",
                    "REQUIRED",
                    "데이트 일정 시각이 필요합니다"
            );
        }
        return scheduledAt;
    }

    private List<Long> validatedPlaceIds(List<Long> placeIds) {
        if (placeIds == null) {
            return List.of();
        }
        if (placeIds.stream().anyMatch(Objects::isNull)) {
            throw new InvalidDateCourseException(
                    "placeIds",
                    "INVALID",
                    "장소 식별자는 null일 수 없습니다"
            );
        }
        long distinctCount = placeIds.stream().distinct().count();
        if (distinctCount != placeIds.size()) {
            throw new InvalidDateCourseException(
                    "placeIds",
                    "DUPLICATED",
                    "데이트 코스 장소는 중복될 수 없습니다"
            );
        }
        return List.copyOf(placeIds);
    }

    private void validateCoupleSavedPlaces(List<Long> coupleMemberIds, List<Long> placeIds) {
        if (placeIds.isEmpty()) {
            return;
        }
        Set<Long> savedPlaceIds = memberPlaceRepository.findAllByMemberIdInOrderBySavedAtDesc(
                        coupleMemberIds
                )
                .stream()
                .map(memberPlace -> memberPlace.getPlace().getId())
                .collect(Collectors.toSet());
        boolean containsUnsaved = placeIds.stream().anyMatch(placeId -> !savedPlaceIds.contains(placeId));
        if (containsUnsaved) {
            throw new DateCoursePlaceNotSavedException();
        }
    }

    private List<DateCoursePlace> placeIdsToCoursePlaces(
            Long dateCourseId,
            List<Long> placeIds,
            Instant now
    ) {
        if (placeIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Place> placeById = placeRepository.findAllById(placeIds)
                .stream()
                .collect(Collectors.toMap(Place::getId, Function.identity(), (left, right) -> left));
        if (placeById.size() != placeIds.size()) {
            throw new PlaceNotFoundException();
        }
        return IntStream.range(0, placeIds.size())
                .mapToObj(index -> DateCoursePlace.create(
                        dateCourseId,
                        placeById.get(placeIds.get(index)),
                        index + 1,
                        now
                ))
                .toList();
    }

    private DateCourseView toView(DateCourse dateCourse, List<DateCoursePlace> places) {
        List<DateCoursePlace> orderedPlaces = places.stream()
                .sorted(java.util.Comparator.comparing(DateCoursePlace::getSequenceOrder))
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

    private record CoupleContext(Long coupleId, List<Long> memberIds) {

        private Long partnerMemberId(Long memberId) {
            return memberIds.stream()
                    .filter(id -> !Objects.equals(id, memberId))
                    .findFirst()
                    .orElseThrow(CoupleStateInvalidException::new);
        }
    }
}
