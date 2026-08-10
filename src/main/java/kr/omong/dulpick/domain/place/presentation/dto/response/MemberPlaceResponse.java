package kr.omong.dulpick.domain.place.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.place.application.MemberPlaceView;
import kr.omong.dulpick.domain.place.domain.PlaceOwnershipStatus;
import kr.omong.dulpick.global.time.ServiceTime;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MemberPlaceResponse(
        @Schema(description = "별칭·메모·저장 시각의 기준 회원 ID. MINE/TOGETHER는 현재 회원, PARTNER는 상대방")
        Long memberId,
        @Schema(description = "공용 장소 ID. 회원별 저장 기록 자체의 ID가 아님")
        Long placeId,
        String name,
        String address,
        String roadAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        @Schema(description = "Kakao가 제공한 원본 카테고리 경로")
        String category,
        @Schema(description = "둘픽 장소 분류", allowableValues = {
                "맛집", "카페", "놀거리", "쇼핑", "생활 편의", "관광", "숙박"
        })
        String categoryName,
        @Schema(description = "현재 커플 기준 저장 관계", allowableValues = {
                "MINE", "PARTNER", "TOGETHER"
        })
        PlaceOwnershipStatus ownershipStatus,
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
                view.categoryName(),
                view.ownershipStatus(),
                view.alias(),
                view.memo(),
                ServiceTime.toLocalDateTime(view.savedAt())
        );
    }
}
