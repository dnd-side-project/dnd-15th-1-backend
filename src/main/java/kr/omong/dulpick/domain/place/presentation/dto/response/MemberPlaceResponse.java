package kr.omong.dulpick.domain.place.presentation.dto.response;

import kr.omong.dulpick.domain.place.application.MemberPlaceView;
import kr.omong.dulpick.global.time.ServiceTime;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MemberPlaceResponse(
        Long memberId,
        Long placeId,
        String name,
        String address,
        String roadAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        String category,
        String alias,
        String memo,
        LocalDateTime savedAt
) {

    public static MemberPlaceResponse from(MemberPlaceView view) {
        return new MemberPlaceResponse(
                view.memberId(),
                view.placeId(),
                view.name(),
                view.address(),
                view.roadAddress(),
                view.latitude(),
                view.longitude(),
                view.category(),
                view.alias(),
                view.memo(),
                ServiceTime.toLocalDateTime(view.savedAt())
        );
    }
}
