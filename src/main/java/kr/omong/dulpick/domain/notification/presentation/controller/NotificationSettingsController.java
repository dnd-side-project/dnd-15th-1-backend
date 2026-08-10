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

@Tag(
        name = SwaggerTagNames.NOTIFICATION,
        description = "알림함 조회·읽음 처리, 알림 수신 설정, iOS 푸시 디바이스 관리 API"
)
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
            summary = "내 알림 수신 설정 조회",
            description = "현재 회원의 알림 수신 설정을 조회합니다. 최초 기본값은 콘텐츠 저장 알림 ON, "
                    + "데이트 일정 알림 ON, 마케팅 알림 OFF입니다."
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
            summary = "내 알림 수신 설정 변경",
            description = "부분 수정이 아닌 전체 교체 API이므로 세 가지 알림 설정을 모두 전달해야 합니다. "
                    + "마케팅 알림을 켤 때는 조회 응답의 availableMarketingConsentVersion을 "
                    + "marketingConsentVersion으로 전달하세요."
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
