package kr.omong.dulpick.domain.notification.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kr.omong.dulpick.domain.notification.application.query.NotificationInboxService;
import kr.omong.dulpick.domain.notification.presentation.dto.response.NotificationPageResponse;
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

@Tag(
        name = SwaggerTagNames.NOTIFICATION,
        description = "알림함 조회·읽음 처리, 알림 수신 설정, iOS 푸시 디바이스 관리 API"
)
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
            summary = "내 알림 목록 조회",
            description = "내 알림을 최신순으로 조회합니다. 첫 요청에서는 cursor를 생략하세요. "
                    + "다음 페이지가 있으면 응답의 nextCursor를 다음 요청의 cursor로 그대로 전달합니다. "
                    + "nextCursor가 null이면 마지막 페이지입니다."
    )
    @GetMapping
    public ResponseEntity<NotificationPageResponse> getNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(
                    description = "다음 페이지 조회 토큰. 첫 요청에서는 생략하고, "
                            + "이후 요청에서는 이전 응답의 nextCursor를 그대로 전달합니다."
            )
            @RequestParam(required = false) String cursor,
            @Parameter(description = "한 번에 조회할 알림 수(기본값 20, 최소 1, 최대 50)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
    ) {
        return ResponseEntity.ok(NotificationPageResponse.from(
                notificationInboxService.getPage(memberId(jwt), cursor, size)
        ));
    }

    @Operation(
            summary = "알림 한 건 읽음 처리",
            description = "내 알림 한 건을 읽음 상태로 변경합니다. 이미 읽은 알림을 다시 요청해도 성공하며, "
                    + "응답 본문 없이 204를 반환합니다."
    )
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markRead(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "읽음 처리할 알림 ID")
            @PathVariable Long notificationId
    ) {
        notificationInboxService.markRead(memberId(jwt), notificationId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "모든 알림 읽음 처리",
            description = "내 알림함의 모든 읽지 않은 알림을 읽음 상태로 변경하고, 응답 본문 없이 204를 반환합니다."
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
