package kr.omong.dulpick.domain.couple.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.omong.dulpick.domain.couple.application.query.ConnectionCodeQueryService;
import kr.omong.dulpick.domain.couple.presentation.dto.ConnectionCodeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "커플 연결 코드")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/connection-codes")
public class ConnectionCodeController {

    private final ConnectionCodeQueryService connectionCodeQueryService;

    public ConnectionCodeController(ConnectionCodeQueryService connectionCodeQueryService) {
        this.connectionCodeQueryService = connectionCodeQueryService;
    }

    @Operation(summary = "내 활성 연결 코드 조회")
    @GetMapping("/me")
    public ResponseEntity<ConnectionCodeResponse> getMyCode(
            @AuthenticationPrincipal Jwt jwt
    ) {
        var code = connectionCodeQueryService.getMyActiveCode(Long.valueOf(jwt.getSubject()));
        return ResponseEntity.ok(ConnectionCodeResponse.from(code));
    }
}
