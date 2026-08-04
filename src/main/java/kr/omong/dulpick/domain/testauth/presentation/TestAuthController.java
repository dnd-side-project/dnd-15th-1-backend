package kr.omong.dulpick.domain.testauth.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;
import kr.omong.dulpick.domain.auth.presentation.dto.request.TokenRefreshRequest;
import kr.omong.dulpick.domain.auth.presentation.dto.response.TokenResponse;
import kr.omong.dulpick.domain.testauth.application.TestAuthResult;
import kr.omong.dulpick.domain.testauth.application.TestAuthService;
import kr.omong.dulpick.domain.testauth.presentation.dto.TestAuthCredentialsRequest;
import kr.omong.dulpick.domain.testauth.presentation.dto.TestAuthResponse;
import kr.omong.dulpick.domain.testauth.security.TestAuthAccessKeyFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "인증2",
        description = "Swagger 및 개발 검증에 사용하는 제거 가능한 자체 인증 기능"
)
@ApiResponses({
        @ApiResponse(
                responseCode = "401",
                description = "X-Test-Auth-Key가 누락되었거나 올바르지 않은 경우"
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
            description = """
                    이메일과 비밀번호로 인증2 전용 회원을 생성합니다.
                    생성된 회원은 KAKAO 소셜 계정으로 연결되며 일반 소셜 로그인 회원과 동일한 Member와 토큰을 사용합니다.
                    응답의 Access Token을 Swagger bearerAuth에 등록하면 보호된 다른 API를 호출할 수 있습니다.
                    """
    )
    @PostMapping("/signup")
    public ResponseEntity<TestAuthResponse> signUp(
            @Valid @RequestBody TestAuthCredentialsRequest request
    ) {
        TestAuthResult result = testAuthService.signUp(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TestAuthResponse.from(result));
    }

    @Operation(
            summary = "인증2 로그인",
            description = """
                    인증2 회원가입에 사용한 이메일과 비밀번호로 로그인합니다.
                    응답의 Access Token과 Refresh Token은 소셜 로그인에서 발급되는 둘픽 토큰과 동일하게 동작합니다.
                    """
    )
    @PostMapping("/login")
    public ResponseEntity<TestAuthResponse> login(
            @Valid @RequestBody TestAuthCredentialsRequest request
    ) {
        TestAuthResult result = testAuthService.login(request.email(), request.password());
        return ResponseEntity.ok(TestAuthResponse.from(result));
    }

    @Operation(
            summary = "인증2 토큰 재발급",
            description = """
                    인증2 회원에게 발급된 Refresh Token을 회전하여 새 토큰 쌍을 발급합니다.
                    사용한 Refresh Token은 즉시 폐기되며 소셜 로그인 회원의 Refresh Token은 이 API에서 사용할 수 없습니다.
                    """
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
            description = """
                    인증2 회원의 Access Token을 Authorization: Bearer 헤더에 넣고 해당 회원의 Refresh Token을 전달합니다.
                    Refresh Token을 폐기하며 현재 Access Token은 만료 시각까지 유효할 수 있습니다.
                    """
    )
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
