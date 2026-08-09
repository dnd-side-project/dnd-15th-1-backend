package kr.omong.dulpick.domain.notification.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.omong.dulpick.domain.notification.application.command.NotificationSettingsService;
import kr.omong.dulpick.domain.notification.application.command.NotificationSettingsView;
import kr.omong.dulpick.domain.notification.presentation.dto.request.NotificationSettingsRequest;
import kr.omong.dulpick.domain.notification.presentation.dto.response.NotificationSettingsResponse;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = SwaggerTagNames.NOTIFICATION, description = "마이페이지 알림 설정과 받은 알림 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/members/me/notification-settings")
public class NotificationSettingsController {

    private final NotificationSettingsService notificationSettingsService;

    public NotificationSettingsController(
            NotificationSettingsService notificationSettingsService
    ) {
        this.notificationSettingsService = notificationSettingsService;
    }

    @Operation(
            summary = "내 알림 설정 조회",
            description = "콘텐츠 저장, 데이트 일정, 마케팅 알림 설정을 조회합니다. 최초 기본값은 각각 ON, ON, OFF입니다."
    )
    @GetMapping
    public ResponseEntity<NotificationSettingsResponse> get(
            @AuthenticationPrincipal Jwt jwt
    ) {
        NotificationSettingsView settings = notificationSettingsService.get(
                memberId(jwt)
        );
        return ResponseEntity.ok(NotificationSettingsResponse.from(settings));
    }

    @Operation(
            summary = "내 알림 설정 수정",
            description = "세 가지 알림 설정을 모두 교체합니다. 마케팅 알림을 켤 때는 최신 동의 버전이 필요합니다."
    )
    @PutMapping
    public ResponseEntity<NotificationSettingsResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody NotificationSettingsRequest request
    ) {
        NotificationSettingsView settings = notificationSettingsService.update(
                memberId(jwt),
                request.toCommand()
        );
        return ResponseEntity.ok(NotificationSettingsResponse.from(settings));
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
