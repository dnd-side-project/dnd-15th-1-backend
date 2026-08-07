package kr.omong.dulpick.domain.auth.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.omong.dulpick.domain.auth.application.command.AuthCommandService;
import kr.omong.dulpick.domain.auth.application.command.result.IssuedNonce;
import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.command.result.SocialLoginResult;
import kr.omong.dulpick.domain.auth.presentation.dto.request.NonceIssueRequest;
import kr.omong.dulpick.domain.auth.presentation.dto.request.SocialLoginRequest;
import kr.omong.dulpick.domain.auth.presentation.dto.request.TokenRefreshRequest;
import kr.omong.dulpick.domain.auth.presentation.dto.response.NonceResponse;
import kr.omong.dulpick.domain.auth.presentation.dto.response.SocialLoginResponse;
import kr.omong.dulpick.domain.auth.presentation.dto.response.TokenResponse;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = SwaggerTagNames.AUTH)
public class AuthController {

    private final AuthCommandService authCommandService;

    public AuthController(AuthCommandService authCommandService) {
        this.authCommandService = authCommandService;
    }

    @Operation(
            summary = "소셜 로그인 nonce 발급",
            description = """
                    소셜 로그인 검증에 사용할 일회성 nonce를 발급합니다.
                    발급된 nonce는 10분 동안 한 번 사용할 수 있습니다.
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
            description = "소셜 제공자의 ID Token과 nonce를 검증하고, 둘픽 인증 토큰과 온보딩 완료 여부를 반환합니다."
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
                    Refresh Token으로 새 Access Token과 Refresh Token을 발급합니다.
                    요청에 사용한 Refresh Token은 다시 사용할 수 없습니다.
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
                    현재 회원에게 발급된 Refresh Token을 폐기합니다.
                    Access Token은 만료 시각까지 유효할 수 있습니다.
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
