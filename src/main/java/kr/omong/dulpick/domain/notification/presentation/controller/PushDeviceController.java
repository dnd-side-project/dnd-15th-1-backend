package kr.omong.dulpick.domain.notification.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = SwaggerTagNames.NOTIFICATION, description = "FCM 디바이스 등록과 받은 알림 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/push-devices")
public class PushDeviceController {

    private final PushDeviceService pushDeviceService;

    public PushDeviceController(PushDeviceService pushDeviceService) {
        this.pushDeviceService = pushDeviceService;
    }

    @Operation(
            summary = "FCM 디바이스 등록·갱신",
            description = "로그인한 iOS 앱 설치의 FCM 등록 토큰을 등록합니다. 같은 deviceId 요청은 최신 토큰으로 갱신됩니다."
    )
    @PutMapping("/{deviceId}")
    public ResponseEntity<PushDeviceResponse> register(
            @AuthenticationPrincipal Jwt jwt,
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
            summary = "현재 디바이스 푸시 등록 해제",
            description = "로그아웃할 iOS 앱 설치를 푸시 발송 대상에서 제외합니다. 이미 해제된 자신의 디바이스 요청은 성공합니다."
    )
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> unregister(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID deviceId
    ) {
        pushDeviceService.unregister(memberId(jwt), deviceId);
        return ResponseEntity.noContent().build();
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
