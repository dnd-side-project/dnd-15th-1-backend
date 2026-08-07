package kr.omong.dulpick.domain.member.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.omong.dulpick.domain.member.application.command.MemberCommandService;
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
import jakarta.validation.Valid;
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

@Tag(name = "회원")
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

    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> me(@AuthenticationPrincipal Jwt jwt) {
        MemberProfileView profile = memberQueryService.getMyProfile(memberId(jwt));
        return ResponseEntity.ok(MemberResponse.from(profile));
    }

    @Operation(summary = "최초 프로필과 데이트 성향 설정")
    @PostMapping("/me/profile")
    public ResponseEntity<InitializedMemberProfileResponse> initializeProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody InitializeMemberProfileRequest request
    ) {
        var profile = memberCommandService.initializeProfile(
                memberId(jwt),
                request.toCommand()
        );
        return ResponseEntity.status(201).body(InitializedMemberProfileResponse.from(profile));
    }

    @Operation(summary = "마이페이지 기본 프로필 수정")
    @PatchMapping("/me/profile")
    public ResponseEntity<UpdatedMemberProfileResponse> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateMemberProfileRequest request
    ) {
        var profile = memberCommandService.updateProfile(memberId(jwt), request.toCommand());
        return ResponseEntity.ok(UpdatedMemberProfileResponse.from(profile));
    }

    @Operation(summary = "마이페이지 데이트 성향 수정")
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
                    회원을 비활성화하고 발급된 Refresh Token을 모두 폐기합니다.
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
