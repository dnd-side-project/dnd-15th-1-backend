package kr.omong.dulpick.domain.member.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.member.application.query.view.MemberProfileView;
import kr.omong.dulpick.domain.member.domain.MemberStatus;
import kr.omong.dulpick.global.time.ServiceTime;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "회원 정보. 모든 시각은 Asia/Seoul 기준입니다.")
public record MemberResponse(
        @Schema(description = "회원 내부 식별자", example = "123")
        Long memberId,
        @Schema(
                description = "회원 상태. ACTIVE는 활성 회원, WITHDRAWN은 탈퇴 회원을 의미합니다.",
                allowableValues = {"ACTIVE", "WITHDRAWN"},
                example = "ACTIVE"
        )
        MemberStatus status,
        @Schema(description = "닉네임과 프로필 아이콘 설정 완료 여부. 데이트 성향 미설정이어도 true일 수 있습니다.", example = "true")
        boolean onboardingCompleted,
        @Schema(
                description = "회원 닉네임. 사용자 인식 문자 기준 1~6자이며 온보딩 전에는 null입니다.",
                example = "둘픽이",
                minLength = 1,
                maxLength = 6,
                nullable = true
        )
        String nickname,
        @Schema(
                description = "iOS 프로필 에셋 번호(1~5)",
                example = "1",
                minimum = "1",
                maximum = "5",
                nullable = true
        )
        Integer profileIcon,
        @Schema(description = "4가지 데이트 성향. 아직 설정하지 않았으면 null입니다.", nullable = true)
        MemberDatePreferencesResponse datePreferences,
        @Schema(description = "회원 생성 시각. Asia/Seoul 기준입니다.", example = "2026-08-16T14:30:00")
        LocalDateTime createdAt,
        @Schema(description = "회원 정보가 마지막으로 변경된 시각. Asia/Seoul 기준입니다.", example = "2026-08-16T14:30:00")
        LocalDateTime updatedAt,
        @Schema(description = "최근 탈퇴 시각. 탈퇴 이력이 없으면 null입니다.", example = "2026-08-20T10:00:00", nullable = true)
        LocalDateTime lastWithdrawnAt,
        @Schema(description = "최근 재가입 시각. 재가입 이력이 없으면 null입니다.", example = "2026-08-21T09:00:00", nullable = true)
        LocalDateTime lastRejoinedAt,
        @Schema(description = "연결된 소셜 계정 목록. provider와 공개 가능한 이메일만 포함합니다.")
        List<MemberSocialAccountResponse> socialAccounts
) {

    public static MemberResponse from(MemberProfileView profile) {
        List<MemberSocialAccountResponse> accounts = profile.socialAccounts()
                .stream()
                .map(MemberSocialAccountResponse::from)
                .toList();
        return new MemberResponse(
                profile.memberId(),
                profile.status(),
                profile.onboardingCompleted(),
                profile.nickname(),
                profile.profileIcon(),
                MemberDatePreferencesResponse.from(profile.datePreferences()),
                ServiceTime.toLocalDateTime(profile.createdAt()),
                ServiceTime.toLocalDateTime(profile.updatedAt()),
                ServiceTime.toLocalDateTime(profile.lastWithdrawnAt()),
                ServiceTime.toLocalDateTime(profile.lastRejoinedAt()),
                accounts
        );
    }
}
