package kr.omong.dulpick.domain.notice.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.omong.dulpick.domain.notice.application.NoticeService;
import kr.omong.dulpick.domain.notice.presentation.dto.request.CreateNoticeRequest;
import kr.omong.dulpick.domain.notice.presentation.dto.request.UpdateNoticeRequest;
import kr.omong.dulpick.domain.notice.presentation.dto.response.NoticeCreateResponse;
import kr.omong.dulpick.domain.notice.presentation.dto.response.NoticePageResponse;
import kr.omong.dulpick.domain.notice.presentation.dto.response.NoticeResponse;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import kr.omong.dulpick.global.exception.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = SwaggerTagNames.OPS, description = "운영자 대시보드·장애 대응 API")
@SecurityRequirement(name = "basicAuth")
@RestController
@RequestMapping("/api/v1/admin/notices")
public class NoticeAdminController {

    private final NoticeService noticeService;

    public NoticeAdminController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @Operation(summary = "운영자 공지사항 목록 조회")
    @GetMapping
    public ResponseEntity<NoticePageResponse> list(
            @Parameter(example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(example = "20") @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(NoticePageResponse.from(noticeService.list(page, size)));
    }

    @Operation(
            summary = "공지사항 작성",
            description = "공지사항을 저장합니다. sendNotification=true이면 전체 활성 회원을 대상으로 알림함 저장과 푸시 큐 등록 작업을 비동기로 시작합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "공지사항 작성 성공"),
            @ApiResponse(responseCode = "401", description = "운영자 인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<NoticeCreateResponse> create(
            @Valid @RequestBody CreateNoticeRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(NoticeCreateResponse.from(noticeService.create(request.toCommand())));
    }

    @Operation(
            summary = "공지사항 수정",
            description = "기존 공지사항을 조용히 수정합니다. 수정 알림은 전송하지 않으며 expectedUpdatedAt으로 동시 수정을 감지합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공지사항 수정 성공"),
            @ApiResponse(responseCode = "409", description = "최신 내용을 다시 조회해야 함",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{noticeId:[0-9]+}")
    public ResponseEntity<NoticeResponse> update(
            @PathVariable @Schema(example = "42") Long noticeId,
            @Valid @RequestBody UpdateNoticeRequest request
    ) {
        return ResponseEntity.ok(NoticeResponse.from(noticeService.update(noticeId, request.toCommand())));
    }
}
