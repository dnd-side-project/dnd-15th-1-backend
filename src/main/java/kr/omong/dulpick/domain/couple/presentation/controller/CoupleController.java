package kr.omong.dulpick.domain.couple.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.omong.dulpick.domain.couple.application.command.ConnectCoupleCommand;
import kr.omong.dulpick.domain.couple.application.command.CoupleCommandService;
import kr.omong.dulpick.domain.couple.application.query.CoupleQueryService;
import kr.omong.dulpick.domain.couple.application.query.view.ConnectionCodePreview;
import kr.omong.dulpick.domain.couple.application.query.view.CoupleConnectionStatus;
import kr.omong.dulpick.domain.couple.presentation.dto.ConnectionCodePreviewResponse;
import kr.omong.dulpick.domain.couple.presentation.dto.ConnectionCodeRequest;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import kr.omong.dulpick.domain.couple.presentation.dto.CoupleConnectionStatusResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = SwaggerTagNames.COUPLE_CONNECTION,
        description = "연결 코드 확인, 커플 연결 확정 및 현재 연결 상태 조회 API"
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1")
public class CoupleController {

    private final CoupleCommandService coupleCommandService;
    private final CoupleQueryService coupleQueryService;

    public CoupleController(
            CoupleCommandService coupleCommandService,
            CoupleQueryService coupleQueryService
    ) {
        this.coupleCommandService = coupleCommandService;
        this.coupleQueryService = coupleQueryService;
    }

    @Operation(
            summary = "연결 코드 상대방 미리보기",
            description = """
                    연결을 확정하기 전에 연결 코드 소유자의 프로필을 조회합니다.
                    회원별 요청 제한은 분당 5회, 시간당 20회입니다.
                    잘못된 코드가 회원별 10분간 5회 누적되면 15분간 차단됩니다.
                    같은 IP에서 잘못된 코드가 시간당 50회 누적돼도 429를 반환합니다.
                    """
    )
    @PostMapping("/couple-connections/preview")
    public ResponseEntity<ConnectionCodePreviewResponse> preview(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ConnectionCodeRequest request,
            HttpServletRequest httpRequest
    ) {
        ConnectionCodePreview preview = coupleQueryService.preview(
                memberId(jwt),
                request.connectionCode(),
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.ok(ConnectionCodePreviewResponse.from(preview));
    }

    @Operation(
            summary = "커플 연결 확정",
            description = """
                    상대방의 활성 연결 코드로 커플 관계를 생성합니다.
                    회원별 요청 제한은 분당 3회, 일일 10회입니다.
                    연결과 연결 해제는 합산하여 회원별 일일 10회까지 가능합니다.
                    잘못된 코드가 회원별 10분간 5회 누적되면 15분간 차단됩니다.
                    같은 IP에서 잘못된 코드가 시간당 50회 누적돼도 429를 반환합니다.
                    """
    )
    @PostMapping("/couples")
    public ResponseEntity<CoupleConnectionStatusResponse> connect(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ConnectionCodeRequest request,
            HttpServletRequest httpRequest
    ) {
        CoupleConnectionStatus status = coupleCommandService.connect(
                memberId(jwt),
                new ConnectCoupleCommand(request.connectionCode()),
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.status(201).body(CoupleConnectionStatusResponse.from(status));
    }

    @Operation(
            summary = "내 커플 연결 상태 조회",
            description = """
                    연결 여부와 나·상대방의 최신 프로필을 조회합니다.
                    미연결이면 partner, connectedAt, daysTogether는 null입니다.
                    """
    )
    @GetMapping("/couples/me")
    public ResponseEntity<CoupleConnectionStatusResponse> getMyStatus(
            @AuthenticationPrincipal Jwt jwt
    ) {
        CoupleConnectionStatus status = coupleQueryService.getMyStatus(memberId(jwt));
        return ResponseEntity.ok(CoupleConnectionStatusResponse.from(status));
    }

    @Operation(
            summary = "커플 연결 해제",
            description = """
                    현재 활성 커플 연결을 해제합니다.
                    활성 상태인 두 회원은 각각 새로운 연결 코드를 발급받을 수 있습니다.
                    연결과 연결 해제는 합산하여 회원별 일일 10회까지 가능하며,
                    제한을 초과하면 429를 반환합니다.
                    """
    )
    @DeleteMapping("/couples/me")
    public ResponseEntity<Void> disconnect(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest
    ) {
        coupleCommandService.disconnect(memberId(jwt), httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
