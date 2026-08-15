package kr.omong.dulpick.domain.member.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeFormat;
import kr.omong.dulpick.domain.member.application.command.InitializedMemberProfile;

public record InitializedMemberProfileResponse(
        @Schema(description = "앞뒤 공백이 제거되어 저장된 닉네임", example = "둘픽이")
        String nickname,
        @Schema(
                description = "iOS 프로필 에셋 번호(1~5)",
                minimum = "1",
                maximum = "5",
                example = "1"
        )
        int profileIcon,
        @Schema(
                description = "저장된 4가지 데이트 성향. 온보딩에서 생략했으면 null입니다.",
                nullable = true
        )
        MemberDatePreferencesResponse datePreferences,
        @Schema(
                description = "상대방에게 전달할 영문 대문자 5자리 연결 코드",
                pattern = ConnectionCodeFormat.NORMALIZED_PATTERN,
                minLength = ConnectionCodeFormat.LENGTH,
                maxLength = ConnectionCodeFormat.LENGTH,
                example = "ABCDE"
        )
        String connectionCode,
        @Schema(description = "iOS 공유 및 딥링크 진입에 사용할 연결 URL", example = "https://dulpick.omong.kr/connect?code=ABCDE")
        String shareUrl
) {

    public static InitializedMemberProfileResponse from(InitializedMemberProfile profile) {
        return new InitializedMemberProfileResponse(
                profile.nickname(),
                profile.profileIcon(),
                MemberDatePreferencesResponse.from(profile.datePreferences()),
                profile.connectionCode().code(),
                profile.connectionCode().shareUrl()
        );
    }
}
