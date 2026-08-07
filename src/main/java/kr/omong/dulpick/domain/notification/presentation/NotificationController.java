package kr.omong.dulpick.domain.notification.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kr.omong.dulpick.domain.notification.application.NotificationInboxService;
import kr.omong.dulpick.domain.notification.presentation.dto.NotificationPageResponse;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = SwaggerTagNames.NOTIFICATION, description = "FCM 디바이스 등록과 받은 알림 관리 API")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationInboxService notificationInboxService;

    public NotificationController(NotificationInboxService notificationInboxService) {
        this.notificationInboxService = notificationInboxService;
    }

    @Operation(
            summary = "받은 알림 목록 조회",
            description = "받은 알림을 최신순으로 조회합니다. nextCursor는 다음 요청에 그대로 전달합니다."
    )
    @GetMapping
    public ResponseEntity<NotificationPageResponse> getNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "이전 응답의 nextCursor")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기(1~50)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
    ) {
        return ResponseEntity.ok(NotificationPageResponse.from(
                notificationInboxService.getPage(memberId(jwt), cursor, size)
        ));
    }

    @Operation(
            summary = "알림 확인 처리",
            description = "내 알림 한 건을 확인 상태로 변경합니다. 이미 확인한 알림에도 동일하게 성공합니다."
    )
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markRead(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long notificationId
    ) {
        notificationInboxService.markRead(memberId(jwt), notificationId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "모든 알림 확인 처리",
            description = "현재 회원의 모든 미확인 알림을 확인 상태로 변경합니다."
    )
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal Jwt jwt) {
        notificationInboxService.markAllRead(memberId(jwt));
        return ResponseEntity.noContent().build();
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
