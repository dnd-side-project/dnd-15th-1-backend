package kr.omong.dulpick.domain.notice.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.omong.dulpick.domain.notice.application.NoticeService;
import kr.omong.dulpick.domain.notice.presentation.dto.response.NoticePageResponse;
import kr.omong.dulpick.domain.notice.presentation.dto.response.NoticeResponse;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import kr.omong.dulpick.global.exception.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = SwaggerTagNames.NOTICE, description = "공지사항 API")
@RestController
@RequestMapping("/api/v1/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @Operation(summary = "공지사항 목록 조회")
    @GetMapping
    public ResponseEntity<NoticePageResponse> list(
            @Parameter(example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(example = "20") @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(NoticePageResponse.from(noticeService.list(page, size)));
    }

    @Operation(summary = "공지사항 상세 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공지사항 조회 성공"),
            @ApiResponse(responseCode = "404", description = "공지사항을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{noticeId:[0-9]+}")
    public ResponseEntity<NoticeResponse> get(@PathVariable Long noticeId) {
        return ResponseEntity.ok(NoticeResponse.from(noticeService.get(noticeId)));
    }
}
