package kr.omong.dulpick.domain.auth.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.omong.dulpick.domain.auth.application.IssuedNonce;
import kr.omong.dulpick.domain.auth.application.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.LoginNonceService;
import kr.omong.dulpick.domain.auth.application.SocialLoginResult;
import kr.omong.dulpick.domain.auth.application.SocialLoginService;
import kr.omong.dulpick.domain.auth.application.TokenService;
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

    private final LoginNonceService loginNonceService;
    private final SocialLoginService socialLoginService;
    private final TokenService tokenService;

    public AuthController(
            LoginNonceService loginNonceService,
            SocialLoginService socialLoginService,
            TokenService tokenService
    ) {
        this.loginNonceService = loginNonceService;
        this.socialLoginService = socialLoginService;
        this.tokenService = tokenService;
    }

    @Operation(
            summary = "소셜 로그인 nonce 발급",
            description = "Google, Kakao, Apple 로그인 전에 사용할 일회성 nonce를 발급합니다."
    )
    @PostMapping("/nonce")
    public ResponseEntity<NonceResponse> issueNonce(
            @Valid @RequestBody NonceIssueRequest request
    ) {
        IssuedNonce issuedNonce = loginNonceService.issue(request.provider());
        return ResponseEntity.ok(NonceResponse.from(issuedNonce));
    }

    @Operation(
            summary = "소셜 로그인",
            description = "발급받은 nonce 원문과 해당 nonce가 포함된 ID Token을 검증합니다."
    )
    @PostMapping("/social-login")
    public ResponseEntity<SocialLoginResponse> socialLogin(
            @Valid @RequestBody SocialLoginRequest request
    ) {
        SocialLoginResult result = socialLoginService.login(request.toCommand());
        return ResponseEntity.ok(SocialLoginResponse.from(result));
    }

    @Operation(summary = "인증 토큰 재발급")
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(
            @Valid @RequestBody TokenRefreshRequest request
    ) {
        IssuedTokens tokens = tokenService.rotate(request.refreshToken());
        return ResponseEntity.ok(TokenResponse.from(tokens));
    }

    @Operation(summary = "로그아웃")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TokenRefreshRequest request
    ) {
        tokenService.revoke(request.refreshToken(), Long.valueOf(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }
}
