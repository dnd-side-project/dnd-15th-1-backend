package kr.omong.dulpick.domain.notification.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.omong.dulpick.domain.notification.application.admin.EmailAnnouncementAdminService;
import kr.omong.dulpick.domain.notification.presentation.dto.request.AddEmailOptOutRequest;
import kr.omong.dulpick.domain.notification.presentation.dto.request.SendEmailAnnouncementRequest;
import kr.omong.dulpick.domain.notification.presentation.dto.response.EmailAnnouncementHistoryResponse;
import kr.omong.dulpick.domain.notification.presentation.dto.response.EmailAnnouncementPreviewResponse;
import kr.omong.dulpick.domain.notification.presentation.dto.response.EmailAnnouncementSendResponse;
import kr.omong.dulpick.domain.notification.presentation.dto.response.EmailOptOutListResponse;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = SwaggerTagNames.OPS, description = "이메일 공지 발송 및 수신 거부 운영 API")
@SecurityRequirement(name = "basicAuth")
@RestController
@RequestMapping("/api/v1/admin/email-announcements")
public class EmailAnnouncementAdminController {

    private final EmailAnnouncementAdminService adminService;

    public EmailAnnouncementAdminController(EmailAnnouncementAdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "이메일 공지 발송 대상 미리보기", description = "활성 회원 중 이메일이 등록된 대상과 수신 거부 제외 결과를 반환합니다.")
    @GetMapping("/preview")
    public ResponseEntity<EmailAnnouncementPreviewResponse> preview() {
        return ResponseEntity.ok(EmailAnnouncementPreviewResponse.from(adminService.previewRecipients()));
    }

    @Operation(
            summary = "이메일 공지 발송 트리거",
            description = "대상에게 공지 메일 발송을 트리거합니다. SMTP 연동 전에는 로그 기록 모드로 처리되며 "
                    + "상태가 COMPLETED_LOG_ONLY로 저장됩니다."
    )
    @PostMapping("/send")
    public ResponseEntity<EmailAnnouncementSendResponse> send(
            @Valid @RequestBody SendEmailAnnouncementRequest request
    ) {
        return ResponseEntity.accepted().body(
                EmailAnnouncementSendResponse.from(adminService.send(request.title(), request.body()))
        );
    }

    @Operation(summary = "이메일 공지 발송 이력 조회")
    @GetMapping("/history")
    public ResponseEntity<EmailAnnouncementHistoryResponse> history(
            @io.swagger.v3.oas.annotations.Parameter(example = "0") @RequestParam(defaultValue = "0") @Schema(example = "0") int page,
            @io.swagger.v3.oas.annotations.Parameter(example = "10") @RequestParam(defaultValue = "10") @Schema(example = "10") int size
    ) {
        return ResponseEntity.ok(
                EmailAnnouncementHistoryResponse.from(adminService.history(page, size), page, size)
        );
    }

    @Operation(summary = "수신 거부 목록 조회")
    @GetMapping("/opt-outs")
    public ResponseEntity<EmailOptOutListResponse> optOuts() {
        return ResponseEntity.ok(EmailOptOutListResponse.from(adminService.optOuts()));
    }

    @Operation(summary = "수신 거부 등록", description = "이미 등록된 회원이면 false를 반환합니다.")
    @PostMapping("/opt-outs")
    public ResponseEntity<java.util.Map<String, Boolean>> addOptOut(
            @Valid @RequestBody AddEmailOptOutRequest request
    ) {
        boolean created = adminService.addOptOut(request.memberId());
        return ResponseEntity.ok(java.util.Map.of("created", created));
    }

    @Operation(summary = "수신 거부 해제")
    @DeleteMapping("/opt-outs/{memberId:[0-9]+}")
    public ResponseEntity<Void> removeOptOut(
            @io.swagger.v3.oas.annotations.Parameter(example = "101") @PathVariable @Schema(example = "101") Long memberId
    ) {
        adminService.removeOptOut(memberId);
        return ResponseEntity.noContent().build();
    }
}
