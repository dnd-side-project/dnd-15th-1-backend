package kr.omong.dulpick.domain.notification.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.omong.dulpick.domain.notification.application.admin.MarketingNotificationAdminService;
import kr.omong.dulpick.domain.notification.presentation.dto.request.MarketingNotificationRequest;
import kr.omong.dulpick.domain.notification.presentation.dto.response.MarketingNotificationResponse;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import kr.omong.dulpick.global.exception.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = SwaggerTagNames.OPS, description = "운영자 대시보드·장애 대응 API")
@SecurityRequirement(name = "basicAuth")
@RestController
@RequestMapping("/api/v1/admin/notifications")
public class NotificationAdminController {

    private final MarketingNotificationAdminService adminService;

    public NotificationAdminController(MarketingNotificationAdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(
            summary = "마케팅 수신 동의 회원에게 알림 발송",
            description = "활성 상태이며 마케팅 알림 수신에 동의한 회원의 알림함에 저장하고, "
                    + "등록된 활성 FCM 디바이스의 푸시 큐에 등록합니다. 실제 FCM 전송은 백그라운드 워커가 수행합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "알림 발송 작업 등록 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "제목 또는 본문이 비어 있거나 길이 제한을 초과함",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "운영자 인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/marketing")
    public ResponseEntity<MarketingNotificationResponse> sendMarketingNotification(
            @Valid @RequestBody MarketingNotificationRequest request
    ) {
        return ResponseEntity.accepted().body(MarketingNotificationResponse.from(
                adminService.send(request.toCommand())
        ));
    }
}
