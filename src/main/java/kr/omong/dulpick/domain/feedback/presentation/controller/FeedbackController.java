package kr.omong.dulpick.domain.feedback.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.omong.dulpick.domain.feedback.application.FeedbackCommandService;
import kr.omong.dulpick.domain.feedback.application.ReceivedFeedback;
import kr.omong.dulpick.domain.feedback.presentation.dto.request.FeedbackRequest;
import kr.omong.dulpick.domain.feedback.presentation.dto.response.FeedbackResponse;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import kr.omong.dulpick.global.exception.ErrorResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = SwaggerTagNames.FEEDBACK, description = "마이페이지 서비스 문의와 개선 의견 접수 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/feedbacks")
public class FeedbackController {

    private final FeedbackCommandService feedbackCommandService;

    public FeedbackController(FeedbackCommandService feedbackCommandService) {
        this.feedbackCommandService = feedbackCommandService;
    }

    @Operation(
            summary = "서비스 피드백 등록",
            description = "문의, 오류 제보, 기능 제안 또는 기타 의견을 접수합니다. 회원별 하루 10회까지 등록할 수 있습니다. clientRequestId는 iOS에서 새 피드백마다 UUID를 생성하고, 동일 요청 재시도 시 같은 값을 사용합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "피드백 접수 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = FeedbackResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "피드백 유형·내용·요청 UUID가 누락되었거나 형식이 올바르지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 없거나 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "회원별 피드백 등록 횟수 제한을 초과했습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<FeedbackResponse> receive(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody FeedbackRequest request
    ) {
        ReceivedFeedback feedback = feedbackCommandService.receive(
                memberId(jwt),
                request.toCommand()
        );
        return ResponseEntity.status(201).body(FeedbackResponse.from(feedback));
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
