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

@Tag(name = "회원", description = "온보딩 프로필 설정과 마이페이지 회원 정보 관리 API")
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
                    로그인 직후 또는 마이페이지 진입 시 현재 회원 상태와 프로필을 조회할 때 사용합니다.

                    onboardingCompleted가 false이면 아직 최초 프로필 설정 전이므로 nickname, profileIcon,
                    datePreferences가 null이며, 프론트는 온보딩 화면으로 이동할 수 있습니다.
                    true이면 서버에 저장된 최신 프로필과 4가지 데이트 성향을 반환합니다.
                    """
    )
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> me(@AuthenticationPrincipal Jwt jwt) {
        MemberProfileView profile = memberQueryService.getMyProfile(memberId(jwt));
        return ResponseEntity.ok(MemberResponse.from(profile));
    }

    @Operation(
            summary = "최초 프로필과 데이트 성향 설정",
            description = """
                    소셜 로그인 후 최초 온보딩에서 닉네임, 프로필 아이콘, 데이트 성향을 처음 저장할 때 사용합니다.

                    닉네임은 앞뒤 공백 제거 후 사용자 인식 문자 기준 1~6자이며 공백만 또는 제어 문자는 허용하지 않습니다.
                    profileIcon은 1~5 중 하나이고, iOS가 이 번호를 앱 내장 그래픽 에셋과 매핑합니다.
                    데이트 성향은 네 범주에서 각각 하나씩 모두 선택해야 합니다.
                    설정 완료 시 상대방에게 공유할 영문 대문자 6자리 연결 코드도 함께 자동 발급됩니다.
                    이미 최초 설정을 완료한 회원은 이 API 대신 마이페이지 프로필/데이트 성향 수정 API를 사용합니다.
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
                    온보딩 완료 후 마이페이지에서 닉네임 또는 프로필 아이콘을 변경할 때 사용합니다.

                    nickname과 profileIcon 중 적어도 하나를 전달해야 하며, 생략한 값은 기존 값을 유지합니다.
                    닉네임은 앞뒤 공백 제거 후 사용자 인식 문자 기준 1~6자이고 profileIcon은 1~5입니다.
                    커플 연결 중 수정해도 상대방이 연결 상태를 다시 조회하면 변경된 최신 프로필이 반환됩니다.
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
                    온보딩 완료 후 마이페이지에서 나의 데이트 성향을 다시 선택할 때 사용합니다.

                    네 범주를 부분 수정하지 않고 항상 모두 전달합니다.
                    indoorOutdoor는 INDOOR/OUTDOOR, activityLevel은 ACTIVE/STATIC,
                    dateTime은 DAY/NIGHT, dateFocus는 FOOD/SIGHTSEEING 중 하나만 허용합니다.
                    각 필드에 다른 범주의 값을 넣으면 유효하지 않은 프로필 요청으로 거부합니다.
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
                    마이페이지에서 사용자가 회원 탈퇴를 최종 확인한 뒤 계정을 비활성화할 때 사용합니다.

                    발급된 Refresh Token을 모두 폐기하고 tokenVersion을 변경해 기존 Access Token을 차단합니다.
                    커플 연결 중이라면 같은 트랜잭션에서 관계를 해제하고 상대방 데이터 접근을 즉시 차단합니다.
                    탈퇴 회원에게는 새 연결 코드를 발급하지 않으며, 활성 상태로 남은 상대방에게만 새 코드를 발급합니다.
                    Apple 철회 정보가 저장된 경우 로컬 탈퇴를 먼저 완료한 뒤 서버가 Apple 연결 해제를 비동기로 재시도합니다.
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
