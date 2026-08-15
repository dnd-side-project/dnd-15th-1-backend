package kr.omong.dulpick.domain.member.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.omong.dulpick.domain.member.application.command.InitializedMemberProfile;
import kr.omong.dulpick.domain.member.application.command.MemberCommandService;
import kr.omong.dulpick.domain.member.application.command.UpdatedMemberProfile;
import kr.omong.dulpick.domain.member.application.query.MemberQueryService;
import kr.omong.dulpick.domain.member.application.query.view.MemberProfileView;
import kr.omong.dulpick.domain.member.domain.DatePreferences;
import kr.omong.dulpick.domain.member.presentation.dto.request.DatePreferencesRequest;
import kr.omong.dulpick.domain.member.presentation.dto.request.InitializeMemberProfileRequest;
import kr.omong.dulpick.domain.member.presentation.dto.request.UpdateMemberProfileRequest;
import kr.omong.dulpick.domain.member.presentation.dto.response.InitializedMemberProfileResponse;
import kr.omong.dulpick.domain.member.presentation.dto.response.MemberDatePreferencesResponse;
import kr.omong.dulpick.domain.member.presentation.dto.response.MemberResponse;
import kr.omong.dulpick.domain.member.presentation.dto.response.UpdatedMemberProfileResponse;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = SwaggerTagNames.MEMBER,
        description = "온보딩 프로필 설정과 마이페이지 회원 정보 관리 API"
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberQueryService memberQueryService;
    private final MemberCommandService memberCommandService;

    public MemberController(
            MemberQueryService memberQueryService,
            MemberCommandService memberCommandService
    ) {
        this.memberQueryService = memberQueryService;
        this.memberCommandService = memberCommandService;
    }

    @Operation(
            summary = "내 정보 조회",
            description = """
                    현재 회원의 상태와 프로필을 조회합니다.
                    onboardingCompleted는 닉네임과 프로필 아이콘이 저장되었는지를 나타냅니다.
                    데이트 성향을 아직 설정하지 않은 회원도 onboardingCompleted=true일 수 있으며,
                    이 경우 datePreferences는 null입니다.
                    """
    )
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> me(@AuthenticationPrincipal Jwt jwt) {
        MemberProfileView profile = memberQueryService.getMyProfile(memberId(jwt));
        return ResponseEntity.ok(MemberResponse.from(profile));
    }

    @Operation(
            summary = "최초 프로필 설정",
            description = """
                    닉네임과 프로필 아이콘을 저장하고 영문 대문자 5자리 연결 코드를 발급합니다.
                    nickname과 profileIcon은 필수입니다. 최초 프로필 아이콘의 앱 기본값은 1입니다.
                    datePreferences는 선택값입니다. 생략하거나 null을 보내면 데이트 성향을 설정하지 않은 상태로 저장하고,
                    응답에서도 null을 반환합니다. 네 가지 성향 중 일부만 입력하면 허용하지 않습니다.
                    데이트 성향을 설정하려면 indoorOutdoor, activityLevel, dateTime, dateFocus를 모두 입력해야 합니다.
                    """
    )
    @PostMapping("/me/profile")
    public ResponseEntity<InitializedMemberProfileResponse> initializeProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody InitializeMemberProfileRequest request
    ) {
        InitializedMemberProfile profile = memberCommandService.initializeProfile(
                memberId(jwt),
                request.toCommand()
        );
        return ResponseEntity.status(201).body(InitializedMemberProfileResponse.from(profile));
    }

    @Operation(
            summary = "마이페이지 기본 프로필 수정",
            description = """
                    닉네임 또는 프로필 아이콘을 변경합니다. 두 필드 중 하나 이상을 입력해야 합니다.
                    생략한 필드는 기존 값을 유지합니다. nickname은 1~6자, profileIcon은 1~5만 허용합니다.
                    데이트 성향은 이 API에서 변경하지 않습니다.
                    """
    )
    @PatchMapping("/me/profile")
    public ResponseEntity<UpdatedMemberProfileResponse> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateMemberProfileRequest request
    ) {
        UpdatedMemberProfile profile = memberCommandService.updateProfile(
                memberId(jwt),
                request.toCommand()
        );
        return ResponseEntity.ok(UpdatedMemberProfileResponse.from(profile));
    }

    @Operation(
            summary = "마이페이지 데이트 성향 수정",
            description = """
                    네 가지 데이트 성향을 한 번에 설정하거나 교체합니다.
                    요청에는 네 필드를 모두 포함해야 하며 각 필드는 아래 값 중 하나여야 합니다.
                    indoorOutdoor: INDOOR(실내), OUTDOOR(실외)
                    activityLevel: ACTIVE(액티비티), STATIC(정적 활동)
                    dateTime: DAY(낮), NIGHT(밤)
                    dateFocus: FOOD(식사 중심), SIGHTSEEING(볼거리 중심)
                    아직 성향을 설정하지 않은 회원도 이 API로 최초 설정할 수 있습니다.
                    """
    )
    @PutMapping("/me/date-preferences")
    public ResponseEntity<MemberDatePreferencesResponse> updateDatePreferences(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DatePreferencesRequest request
    ) {
        DatePreferences preferences = memberCommandService.updateDatePreferences(
                memberId(jwt),
                request.toDomain()
        );
        return ResponseEntity.ok(MemberDatePreferencesResponse.from(preferences));
    }

    @Operation(
            summary = "회원 탈퇴",
            description = """
                    현재 회원을 탈퇴 처리하고 발급된 인증 토큰을 무효화합니다.
                    활성 커플이 있으면 연결도 함께 해제됩니다.
                    """
    )
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal Jwt jwt) {
        memberCommandService.withdraw(memberId(jwt));
        return ResponseEntity.noContent().build();
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
