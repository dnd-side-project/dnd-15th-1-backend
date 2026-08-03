package kr.omong.dulpick.domain.auth.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.omong.dulpick.domain.auth.application.IssuedNonce;
import kr.omong.dulpick.domain.auth.application.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.SocialLoginResult;
import kr.omong.dulpick.domain.auth.application.command.AuthCommandService;
import kr.omong.dulpick.domain.auth.presentation.dto.request.NonceIssueRequest;
import kr.omong.dulpick.domain.auth.presentation.dto.request.SocialLoginRequest;
import kr.omong.dulpick.domain.auth.presentation.dto.request.TokenRefreshRequest;
import kr.omong.dulpick.domain.auth.presentation.dto.response.NonceResponse;
import kr.omong.dulpick.domain.auth.presentation.dto.response.SocialLoginResponse;
import kr.omong.dulpick.domain.auth.presentation.dto.response.TokenResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "인증")
public class AuthController {

    private final AuthCommandService authCommandService;

    public AuthController(AuthCommandService authCommandService) {
        this.authCommandService = authCommandService;
    }

    @Operation(
            summary = "소셜 로그인 nonce 발급",
            description = """
                    소셜 로그인 요청 전에 provider별 일회성 nonce를 발급합니다.

                    provider는 KAKAO(카카오), GOOGLE(구글), APPLE(애플) 중 하나를 사용합니다.
                    응답의 nonce 원문은 발급 후 10분 동안 한 번만 사용할 수 있습니다.
                    expiresAt은 대한민국 표준시(UTC+9, Asia/Seoul) 기준입니다.
                    Google과 Kakao 인증 요청에는 nonce 원문을 전달합니다.
                    Apple 인증 요청에는 nonce 원문의 SHA-256 해시를 전달합니다.
                    이후 소셜 로그인 API의 nonce 필드에는 provider와 관계없이 원문을 전달합니다.
                    """
    )
    @PostMapping("/nonce")
    public ResponseEntity<NonceResponse> issueNonce(
            @Valid @RequestBody NonceIssueRequest request
    ) {
        IssuedNonce issuedNonce = authCommandService.issueNonce(request.provider());
        return ResponseEntity.ok(NonceResponse.from(issuedNonce));
    }

    @Operation(
            summary = "소셜 로그인",
            description = """
                    외부 제공자의 ID Token과 일회성 nonce를 검증하고 둘픽 Access Token과 Refresh Token을 발급합니다.

                    호출 순서:
                    1. nonce 발급 API에서 동일한 provider의 nonce를 발급받습니다.
                    2. 해당 nonce를 사용해 provider SDK 로그인을 수행합니다.
                    3. SDK가 반환한 ID Token과 nonce 원문을 이 API에 전달합니다.

                    provider는 KAKAO(카카오), GOOGLE(구글), APPLE(애플) 중 하나를 사용합니다.
                    Google과 Kakao는 authorizationCode를 보내지 않습니다.
                    Apple authorizationCode는 선택값이지만, 전달하면 서버가 Apple refresh token으로 교환하여
                    암호화 저장하고 회원 탈퇴 시 Apple 연결 철회에 사용합니다.
                    Apple ID Token audience는 com.dulpick.app 또는 com.dulpick.dev만 허용합니다.

                    이 공개 인증 API에는 Authorization 헤더를 보내지 않습니다.
                    성공 응답의 token은 외부 제공자 토큰이 아니라 둘픽 API 전용 토큰입니다.
                    """
    )
    @PostMapping("/social-login")
    public ResponseEntity<SocialLoginResponse> socialLogin(
            @Valid @RequestBody SocialLoginRequest request
    ) {
        SocialLoginResult result = authCommandService.socialLogin(request.toCommand());
        return ResponseEntity.ok(SocialLoginResponse.from(result));
    }

    @Operation(
            summary = "인증 토큰 재발급",
            description = """
                    유효한 둘픽 Refresh Token을 회전하여 새 Access Token과 Refresh Token을 발급합니다.
                    사용한 Refresh Token은 즉시 폐기되므로 재사용할 수 없습니다.
                    이 공개 인증 API에는 Authorization 헤더를 보내지 않습니다.
                    """
    )
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(
            @Valid @RequestBody TokenRefreshRequest request
    ) {
        IssuedTokens tokens = authCommandService.reissue(request.refreshToken());
        return ResponseEntity.ok(TokenResponse.from(tokens));
    }

    @Operation(
            summary = "로그아웃",
            description = """
                    Authorization 헤더의 둘픽 Access Token으로 회원을 확인한 뒤 요청 본문의 Refresh Token을 폐기합니다.
                    요청한 회원에게 발급된 Refresh Token만 폐기할 수 있습니다.
                    현재 Access Token은 만료 시각까지 유효할 수 있습니다.
                    """
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TokenRefreshRequest request
    ) {
        authCommandService.logout(
                request.refreshToken(),
                Long.valueOf(jwt.getSubject())
        );
        return ResponseEntity.noContent().build();
    }
}
