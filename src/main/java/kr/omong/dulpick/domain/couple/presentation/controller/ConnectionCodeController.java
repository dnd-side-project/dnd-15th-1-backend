package kr.omong.dulpick.domain.couple.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.omong.dulpick.domain.couple.application.query.ConnectionCodeQueryService;
import kr.omong.dulpick.domain.couple.application.support.IssuedConnectionCode;
import kr.omong.dulpick.domain.couple.presentation.dto.ConnectionCodeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "커플 연결", description = "상대방에게 전달할 내 연결 코드 조회 API")
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
                    온보딩 완료 후 연결 화면에서 상대방에게 보여주거나 공유할 내 연결 코드를 조회할 때 사용합니다.

                    코드는 영문 대문자 6자리이며 커플 연결이 성립되기 전까지만 유효합니다.
                    최초 프로필 설정 전이거나 이미 커플로 연결된 회원은 활성 코드를 조회할 수 없습니다.
                    응답의 shareUrl은 iOS 딥링크 공유 화면에 사용할 수 있습니다.
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
