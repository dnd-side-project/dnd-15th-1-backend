package kr.omong.dulpick.domain.member.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.member.application.query.view.MemberProfile;
import kr.omong.dulpick.domain.member.domain.MemberStatus;
import kr.omong.dulpick.global.time.ServiceTime;

import java.time.LocalDateTime;
import java.util.List;

public record MemberResponse(
        Long memberId,
        @Schema(
                description = "회원 상태. ACTIVE는 활성 회원, WITHDRAWN은 탈퇴 회원을 의미합니다.",
                allowableValues = {"ACTIVE", "WITHDRAWN"},
                example = "ACTIVE"
        )
        MemberStatus status,
        @Schema(description = "회원 생성 시각. 대한민국 표준시(UTC+9, Asia/Seoul) 기준입니다.")
        LocalDateTime createdAt,
        @Schema(description = "회원 정보 수정 시각. 대한민국 표준시(UTC+9, Asia/Seoul) 기준입니다.")
        LocalDateTime updatedAt,
        @Schema(description = "최근 탈퇴 시각. 대한민국 표준시(UTC+9, Asia/Seoul) 기준입니다.")
        LocalDateTime lastWithdrawnAt,
        @Schema(description = "최근 재가입 시각. 대한민국 표준시(UTC+9, Asia/Seoul) 기준입니다.")
        LocalDateTime lastRejoinedAt,
        List<MemberSocialAccountResponse> socialAccounts
) {

    public static MemberResponse from(MemberProfile profile) {
        List<MemberSocialAccountResponse> accounts = profile.socialAccounts()
                .stream()
                .map(MemberSocialAccountResponse::from)
                .toList();
        return new MemberResponse(
                profile.memberId(),
                profile.status(),
                ServiceTime.toLocalDateTime(profile.createdAt()),
                ServiceTime.toLocalDateTime(profile.updatedAt()),
                ServiceTime.toLocalDateTime(profile.lastWithdrawnAt()),
                ServiceTime.toLocalDateTime(profile.lastRejoinedAt()),
                accounts
        );
    }
}
