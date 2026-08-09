package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMember;
import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMemberRepository;
import kr.omong.dulpick.domain.place.domain.MemberPlace;
import kr.omong.dulpick.domain.place.domain.MemberPlaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class PlaceQueryService {

    private final MemberPlaceRepository memberPlaceRepository;
    private final ActiveCoupleMemberRepository activeCoupleMemberRepository;

    public PlaceQueryService(
            MemberPlaceRepository memberPlaceRepository,
            ActiveCoupleMemberRepository activeCoupleMemberRepository
    ) {
        this.memberPlaceRepository = memberPlaceRepository;
        this.activeCoupleMemberRepository = activeCoupleMemberRepository;
    }

    @Transactional(readOnly = true)
    public List<MemberPlaceView> getVisiblePlaces(Long memberId) {
        List<Long> memberIds = new ArrayList<>();
        memberIds.add(memberId);
        activeCoupleMemberRepository.findByMemberId(memberId)
                .map(ActiveCoupleMember::getCouple)
                .map(couple -> activeCoupleMemberRepository.findAllByCoupleId(couple.getId()))
                .ifPresent(members -> members.stream()
                        .map(ActiveCoupleMember::getMemberId)
                        .filter(id -> !id.equals(memberId))
                        .findFirst()
                        .ifPresent(memberIds::add));
        return memberPlaceRepository.findAllByMemberIdInOrderBySavedAtDesc(memberIds)
                .stream()
                .map(this::toView)
                .toList();
    }

    private MemberPlaceView toView(MemberPlace memberPlace) {
        return new MemberPlaceView(
                memberPlace.getMemberId(),
                memberPlace.getPlace().getId(),
                memberPlace.getPlace().getName(),
                memberPlace.getPlace().getAddress(),
                memberPlace.getPlace().getRoadAddress(),
                memberPlace.getPlace().getLatitude(),
                memberPlace.getPlace().getLongitude(),
                memberPlace.getPlace().getCategory(),
                memberPlace.getAlias(),
                memberPlace.getMemo(),
                memberPlace.getSavedAt()
        );
    }
}
