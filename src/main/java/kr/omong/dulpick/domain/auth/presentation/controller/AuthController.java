package kr.omong.dulpick.domain.auth.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import kr.omong.dulpick.global.exception.ErrorResponse;
import org.springframework.http.MediaType;
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
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "nonce 발급 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = NonceResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "소셜 제공자가 누락되었거나 허용되지 않은 값입니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
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
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "소셜 로그인 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SocialLoginResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "필수 인증값이 누락되었거나 요청 형식이 올바르지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "소셜 로그인 토큰 검증에 실패했습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
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
                    정상적인 중복 요청은 짧은 유예 시간 동안 같은 토큰 교체 결과를 반환합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "인증 토큰 재발급 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TokenResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Refresh Token이 누락되었습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Refresh Token이 유효하지 않거나 만료되었습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
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
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "로그아웃 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Refresh Token이 누락되었습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token 또는 Refresh Token이 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
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
