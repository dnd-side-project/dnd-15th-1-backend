package kr.omong.dulpick.domain.notification.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.omong.dulpick.domain.notification.application.command.PushDeviceService;
import kr.omong.dulpick.domain.notification.application.command.PushDeviceView;
import kr.omong.dulpick.domain.notification.presentation.dto.request.PushDeviceRequest;
import kr.omong.dulpick.domain.notification.presentation.dto.response.PushDeviceResponse;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(
        name = SwaggerTagNames.NOTIFICATION,
        description = "알림함 조회·읽음 처리, 알림 수신 설정, iOS 푸시 디바이스 관리 API"
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/push-devices")
public class PushDeviceController {

    private final PushDeviceService pushDeviceService;

    public PushDeviceController(PushDeviceService pushDeviceService) {
        this.pushDeviceService = pushDeviceService;
    }

    @Operation(
            summary = "현재 iOS 디바이스의 푸시 정보 등록·갱신",
            description = "로그인한 회원에게 현재 앱 설치의 FCM 토큰을 등록합니다. "
                    + "앱 설치별로 생성한 동일한 deviceId를 계속 사용하며, FCM 토큰이 변경되면 같은 API로 갱신합니다."
    )
    @PutMapping("/{deviceId}")
    public ResponseEntity<PushDeviceResponse> register(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "앱 설치 시 생성하고 유지하는 디바이스 UUID")
            @PathVariable UUID deviceId,
            @Valid @RequestBody PushDeviceRequest request
    ) {
        PushDeviceView device = pushDeviceService.register(
                memberId(jwt),
                request.toCommand(deviceId)
        );
        return ResponseEntity.ok(PushDeviceResponse.from(device));
    }

    @Operation(
            summary = "현재 iOS 디바이스의 푸시 등록 해제",
            description = "로그아웃하는 현재 앱 설치를 푸시 발송 대상에서 제외합니다. "
                    + "이미 해제된 내 디바이스를 다시 요청해도 성공하며, 응답 본문 없이 204를 반환합니다."
    )
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> unregister(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "푸시 등록을 해제할 디바이스 UUID")
            @PathVariable UUID deviceId
    ) {
        pushDeviceService.unregister(memberId(jwt), deviceId);
        return ResponseEntity.noContent().build();
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
