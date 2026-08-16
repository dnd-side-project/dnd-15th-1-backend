package kr.omong.dulpick.domain.place.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.omong.dulpick.domain.place.application.PlaceCommandService;
import kr.omong.dulpick.domain.place.application.PlaceImportSubmissionView;
import kr.omong.dulpick.domain.place.application.PlaceImportQueryService;
import kr.omong.dulpick.domain.place.application.PlaceImportService;
import kr.omong.dulpick.domain.place.application.PlaceImportView;
import kr.omong.dulpick.domain.place.domain.PlaceImportStatus;
import kr.omong.dulpick.domain.place.presentation.dto.request.PlaceConfirmRequest;
import kr.omong.dulpick.domain.place.presentation.dto.request.PlaceImportRequest;
import kr.omong.dulpick.domain.place.presentation.dto.response.PlaceConfirmResponse;
import kr.omong.dulpick.domain.place.presentation.dto.response.PlaceImportResponse;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import kr.omong.dulpick.global.exception.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@Tag(name = SwaggerTagNames.PLACE, description = "Instagram·Naver·Tistory 콘텐츠 기반 장소 분석 및 저장 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/place-imports")
public class PlaceImportController {

    private final PlaceImportService placeImportService;
    private final PlaceImportQueryService placeImportQueryService;
    private final PlaceCommandService placeCommandService;

    public PlaceImportController(
            PlaceImportService placeImportService,
            PlaceImportQueryService placeImportQueryService,
            PlaceCommandService placeCommandService
    ) {
        this.placeImportService = placeImportService;
        this.placeImportQueryService = placeImportQueryService;
        this.placeCommandService = placeCommandService;
    }

    @Operation(
            summary = "콘텐츠 링크 장소 분석 요청",
            description = "Instagram 게시물·릴스, Naver 지도·블로그·단축 링크, Tistory 링크를 지원합니다. "
                    + "분석 작업만 등록하며 Gemini·Kakao 호출은 백그라운드에서 수행합니다. "
                    + "202 응답의 Location과 retryAfterSeconds를 사용해 결과를 조회합니다. "
                    + "이미 완료된 동일 작업은 200으로 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "캐시된 완료 분석 결과 반환",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PlaceImportResponse.class))
            ),
            @ApiResponse(
                    responseCode = "202",
                    description = "분석 작업 생성 또는 재처리 대기",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PlaceImportResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "분석할 URL이 누락되었거나 허용 형식이 아닙니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 없거나 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "일일 장소 분석 요청 한도를 초과했습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<PlaceImportResponse> importContent(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PlaceImportRequest request
    ) {
        PlaceImportSubmissionView submission = placeImportService.importLink(
                memberId(jwt),
                request.sourceUrl()
        );
        PlaceImportView placeImport = submission.placeImport();
        return ResponseEntity.status(responseStatus(submission))
                .location(URI.create("/api/v1/place-imports/" + placeImport.importId()))
                .body(PlaceImportResponse.from(placeImport));
    }

    @Operation(
            summary = "장소 분석 결과 조회",
            description = "본인이 요청한 장소 분석 작업의 상태와 Kakao 검증 완료 후보를 조회합니다. "
                    + "status와 nextAction을 기준으로 대기·후보 선택·재시도 여부를 판단합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "장소 분석 결과 조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PlaceImportResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 없거나 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "다른 회원의 분석 작업에는 접근할 수 없습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "분석 작업을 찾을 수 없습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{importId}")
    public ResponseEntity<PlaceImportResponse> get(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "회원별 장소 분석 작업 ID", required = true, example = "1001")
            @PathVariable @Schema(example = "1001") Long importId
    ) {
        return ResponseEntity.ok(PlaceImportResponse.from(
                placeImportQueryService.get(memberId(jwt), importId)
        ));
    }

    @Operation(
            summary = "검증된 장소 저장",
            description = "분석 결과에서 선택한 장소를 회원 저장 목록에 추가하고, 연결 중인 상대방에게 공유합니다. "
                    + "선택 항목의 alias만 저장할 수 있으며 memo 필드는 지원하지 않습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "선택한 장소 저장 완료",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PlaceConfirmResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "후보 목록이 비어 있거나 candidateId·alias 값이 올바르지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 없거나 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "분석 작업을 찾을 수 없습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "선택한 장소 중 현재 회원이 이미 저장한 장소가 있습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "현재 분석 작업에 속하지 않거나 검증 완료 상태가 아닌 후보입니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/{importId}/confirm")
    public ResponseEntity<PlaceConfirmResponse> confirm(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "회원별 장소 분석 작업 ID", required = true, example = "1001")
            @PathVariable @Schema(example = "1001") Long importId,
            @Valid @RequestBody PlaceConfirmRequest request
    ) {
        List<PlaceCommandService.PlaceSelection> selections = request.selections().stream()
                .map(selection -> new PlaceCommandService.PlaceSelection(
                        selection.candidateId(),
                        selection.alias()
                ))
                .toList();
        return ResponseEntity.ok(PlaceConfirmResponse.from(
                placeCommandService.confirm(memberId(jwt), importId, selections)
        ));
    }

    private HttpStatus responseStatus(PlaceImportSubmissionView submission) {
        PlaceImportStatus status = submission.placeImport().status();
        if (status == PlaceImportStatus.RECEIVED || status == PlaceImportStatus.PROCESSING) {
            return HttpStatus.ACCEPTED;
        }
        return HttpStatus.OK;
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
