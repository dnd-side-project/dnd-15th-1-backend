package kr.omong.dulpick.domain.couple.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.omong.dulpick.domain.couple.application.query.ConnectionCodeQueryService;
import kr.omong.dulpick.domain.couple.application.support.IssuedConnectionCode;
import kr.omong.dulpick.domain.couple.presentation.dto.ConnectionCodeResponse;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = SwaggerTagNames.COUPLE_CONNECTION,
        description = "상대방에게 전달할 내 연결 코드 조회 API"
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/connection-codes")
public class ConnectionCodeController {

    private final ConnectionCodeQueryService connectionCodeQueryService;

    public ConnectionCodeController(ConnectionCodeQueryService connectionCodeQueryService) {
        this.connectionCodeQueryService = connectionCodeQueryService;
    }

    @Operation(
            summary = "내 활성 연결 코드 조회",
            description = """
                    상대방에게 전달할 영문 대문자 5자리 연결 코드와 공유 URL을 조회합니다.
                    연결 코드가 없는 회원은 조회할 수 없습니다.
                    """
    )
    @GetMapping("/me")
    public ResponseEntity<ConnectionCodeResponse> getMyCode(
            @AuthenticationPrincipal Jwt jwt
    ) {
        IssuedConnectionCode code = connectionCodeQueryService.getMyActiveCode(
                Long.valueOf(jwt.getSubject())
        );
        return ResponseEntity.ok(ConnectionCodeResponse.from(code));
    }
}
