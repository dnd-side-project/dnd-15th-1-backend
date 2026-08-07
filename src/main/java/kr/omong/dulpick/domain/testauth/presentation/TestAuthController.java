package kr.omong.dulpick.domain.testauth.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.command.result.SocialLoginResult;
import kr.omong.dulpick.domain.auth.presentation.dto.request.TokenRefreshRequest;
import kr.omong.dulpick.domain.auth.presentation.dto.response.SocialLoginResponse;
import kr.omong.dulpick.domain.auth.presentation.dto.response.TokenResponse;
import kr.omong.dulpick.domain.testauth.application.TestAuthService;
import kr.omong.dulpick.domain.testauth.presentation.dto.TestAuthCredentialsRequest;
import kr.omong.dulpick.domain.testauth.security.TestAuthAccessKeyFilter;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import kr.omong.dulpick.global.exception.ErrorResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = SwaggerTagNames.TEST_AUTH,
        description = "Swagger 및 개발 검증에 사용하는 제거 가능한 자체 인증 기능"
)
@ApiResponses({
        @ApiResponse(
                responseCode = "401",
                description = "X-Test-Auth-Key가 누락되었거나 올바르지 않은 경우",
                content = @Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = ErrorResponse.class)
                )
        )
})
@SecurityRequirement(name = TestAuthSwaggerConfig.SECURITY_SCHEME)
@RestController
@RequestMapping(TestAuthController.BASE_PATH)
@ConditionalOnProperty(
        prefix = "features.test-auth",
        name = "enabled",
        havingValue = "true"
)
public class TestAuthController {

    public static final String BASE_PATH = "/api/v1/test-auth";

    private final TestAuthService testAuthService;

    public TestAuthController(TestAuthService testAuthService) {
        this.testAuthService = testAuthService;
    }

    @Operation(
            summary = "인증2 회원가입",
            description = "로컬 개발용 회원을 생성하고 실제 소셜 로그인과 동일한 형식으로 인증 결과를 반환합니다."
    )
    @ApiResponse(
            responseCode = "201",
            description = "회원 생성 및 인증 성공",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SocialLoginResponse.class)
            )
    )
    @PostMapping("/signup")
    public ResponseEntity<SocialLoginResponse> signUp(
            @Valid @RequestBody TestAuthCredentialsRequest request
    ) {
        SocialLoginResult result = testAuthService.signUp(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SocialLoginResponse.from(result));
    }

    @Operation(
            summary = "인증2 로그인",
            description = "인증2 회원을 로그인하고 실제 소셜 로그인과 동일한 형식으로 인증 결과를 반환합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "인증 성공",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SocialLoginResponse.class)
            )
    )
    @PostMapping("/login")
    public ResponseEntity<SocialLoginResponse> login(
            @Valid @RequestBody TestAuthCredentialsRequest request
    ) {
        SocialLoginResult result = testAuthService.login(request.email(), request.password());
        return ResponseEntity.ok(SocialLoginResponse.from(result));
    }

    @Operation(
            summary = "인증2 토큰 재발급",
            description = """
                    인증2 Refresh Token으로 새 토큰을 발급합니다.
                    요청에 사용한 Refresh Token은 다시 사용할 수 없습니다.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "토큰 재발급 성공",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = TokenResponse.class)
            )
    )
    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(
            @Valid @RequestBody TokenRefreshRequest request
    ) {
        IssuedTokens tokens = testAuthService.reissue(request.refreshToken());
        return ResponseEntity.ok(TokenResponse.from(tokens));
    }

    @Operation(
            summary = "인증2 로그아웃",
            description = "현재 인증2 회원에게 발급된 Refresh Token을 폐기합니다."
    )
    @ApiResponse(responseCode = "204", description = "로그아웃 성공")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TokenRefreshRequest request
    ) {
        testAuthService.logout(request.refreshToken(), Long.valueOf(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }
}
