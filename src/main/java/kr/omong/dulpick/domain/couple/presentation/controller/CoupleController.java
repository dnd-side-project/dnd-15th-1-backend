package kr.omong.dulpick.domain.couple.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.omong.dulpick.domain.couple.application.command.ConnectCoupleCommand;
import kr.omong.dulpick.domain.couple.application.command.CoupleCommandService;
import kr.omong.dulpick.domain.couple.application.query.CoupleQueryService;
import kr.omong.dulpick.domain.couple.application.query.view.ConnectionCodePreview;
import kr.omong.dulpick.domain.couple.application.query.view.CoupleConnectionStatus;
import kr.omong.dulpick.domain.couple.presentation.dto.ConnectionCodePreviewResponse;
import kr.omong.dulpick.domain.couple.presentation.dto.ConnectionCodeRequest;
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

@Tag(name = "커플 연결", description = "연결 코드 확인, 커플 연결 확정 및 현재 연결 상태 조회 API")
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
                    연결 코드 입력 화면에서 실제 연결을 확정하기 전에 코드 소유자가 맞는지 확인할 때 사용합니다.

                    활성 연결 코드 소유자의 닉네임과 프로필 아이콘 번호만 반환하며 커플 관계는 생성하지 않습니다.
                    영문 6자리 코드는 소문자로 입력해도 대문자로 정규화합니다.
                    자기 코드, 사용·무효 코드, 이미 연결된 회원의 요청은 거부합니다.
                    미리보기 성공 후 사용자가 확인 버튼을 누르면 커플 연결 확정 API를 호출합니다.
                    """
    )
    @PostMapping("/couple-connections/preview")
    public ResponseEntity<ConnectionCodePreviewResponse> preview(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ConnectionCodeRequest request
    ) {
        ConnectionCodePreview preview = coupleQueryService.preview(
                memberId(jwt),
                request.connectionCode()
        );
        return ResponseEntity.ok(ConnectionCodePreviewResponse.from(preview));
    }

    @Operation(
            summary = "커플 연결 확정",
            description = """
                    연결 코드 미리보기에서 상대방을 확인한 뒤 실제 커플 관계를 생성할 때 사용합니다.

                    상대방의 활성 연결 코드로 두 미연결 회원을 연결합니다.
                    양쪽 회원 모두 최초 프로필 설정을 완료해야 하며 한 회원은 동시에 하나의 활성 커플에만 속할 수 있습니다.
                    성공하면 양쪽 기존 연결 코드는 즉시 무효화되고 연결일을 1일째로 한 상태 응답을 반환합니다.
                    같은 코드를 동시에 사용하더라도 하나의 요청만 성공하도록 트랜잭션과 제약 조건으로 보호합니다.
                    """
    )
    @PostMapping("/couples")
    public ResponseEntity<CoupleConnectionStatusResponse> connect(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ConnectionCodeRequest request
    ) {
        CoupleConnectionStatus status = coupleCommandService.connect(
                memberId(jwt),
                new ConnectCoupleCommand(request.connectionCode())
        );
        return ResponseEntity.status(201).body(CoupleConnectionStatusResponse.from(status));
    }

    @Operation(
            summary = "내 커플 연결 상태 조회",
            description = """
                    앱 실행 후 연결 화면 또는 마이페이지의 커플 영역을 구성할 때 가장 먼저 사용합니다.

                    별도 API를 조합하지 않아도 되도록 연결 여부, 내 최신 프로필, 상대방 최신 프로필,
                    연결 시각과 함께한 일수를 한 번에 반환합니다.
                    미연결이어도 200을 반환하며 partner, connectedAt, daysTogether는 null입니다.
                    함께한 일수는 Asia/Seoul 날짜 기준으로 연결일을 1일째로 계산합니다.
                    프로필 수정 후 다시 호출하면 양쪽 모두 현재 저장된 최신 닉네임과 아이콘 번호가 반영됩니다.
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
                    연결 화면 또는 마이페이지에서 사용자가 상대방과의 연결 해제를 최종 확인한 뒤 사용합니다.

                    한 명의 요청만으로 현재 활성 커플 관계를 종료하며 상대방 승인은 필요하지 않습니다.
                    커플 이력은 DISCONNECTED 상태로 남기고 현재 멤버십을 제거해 상대방 데이터 접근을 즉시 차단합니다.
                    양쪽 활성 회원에게는 과거 코드를 재사용하지 않고 새로운 영문 대문자 6자리 코드를 발급합니다.
                    새 코드는 연결 해제 성공 후 내 활성 연결 코드 조회 API에서 가져옵니다.
                    활성 커플 관계가 없는 상태에서 반복 호출하면 404를 반환하고 코드를 다시 발급하지 않습니다.
                    """
    )
    @DeleteMapping("/couples/me")
    public ResponseEntity<Void> disconnect(@AuthenticationPrincipal Jwt jwt) {
        coupleCommandService.disconnect(memberId(jwt));
        return ResponseEntity.noContent().build();
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
