package kr.omong.dulpick.domain.place.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.omong.dulpick.domain.place.application.PlaceCommandService;
import kr.omong.dulpick.domain.place.application.PlaceImportSubmissionView;
import kr.omong.dulpick.domain.place.application.PlaceImportService;
import kr.omong.dulpick.domain.place.application.PlaceImportView;
import kr.omong.dulpick.domain.place.domain.PlaceImportStatus;
import kr.omong.dulpick.domain.place.presentation.dto.request.PlaceConfirmRequest;
import kr.omong.dulpick.domain.place.presentation.dto.request.PlaceImportRequest;
import kr.omong.dulpick.domain.place.presentation.dto.response.PlaceConfirmResponse;
import kr.omong.dulpick.domain.place.presentation.dto.response.PlaceImportResponse;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import org.springframework.http.HttpStatus;
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

@Tag(name = SwaggerTagNames.PLACE, description = "Instagram 콘텐츠 기반 장소 분석 및 저장 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/place-imports")
public class PlaceImportController {

    private final PlaceImportService placeImportService;
    private final PlaceCommandService placeCommandService;

    public PlaceImportController(
            PlaceImportService placeImportService,
            PlaceCommandService placeCommandService
    ) {
        this.placeImportService = placeImportService;
        this.placeCommandService = placeCommandService;
    }

    @Operation(
            summary = "Instagram 게시물·릴스 장소 분석 요청",
            description = "분석 작업만 등록하며 Gemini·Kakao 호출은 백그라운드에서 수행합니다. 202 응답의 Location과 retryAfterSeconds를 사용해 결과를 조회합니다. 이미 완료된 동일 작업은 200으로 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "캐시된 완료 분석 결과 반환"),
            @ApiResponse(responseCode = "202", description = "분석 작업 생성 또는 재처리 대기")
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
            description = "본인이 요청한 장소 분석 작업의 상태와 Kakao 검증 완료 후보를 조회합니다."
    )
    @GetMapping("/{importId}")
    public ResponseEntity<PlaceImportResponse> get(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "회원별 장소 분석 작업 ID")
            @PathVariable Long importId
    ) {
        return ResponseEntity.ok(PlaceImportResponse.from(
                placeImportService.get(memberId(jwt), importId)
        ));
    }

    @Operation(
            summary = "검증된 장소 저장",
            description = "분석 결과에서 선택한 장소를 회원 저장 목록에 추가하고, 연결 중인 상대방에게 공유합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "선택한 장소 저장 완료"),
            @ApiResponse(responseCode = "409", description = "현재 회원이 이미 저장한 장소"),
            @ApiResponse(responseCode = "422", description = "현재 분석 작업에서 선택할 수 없는 후보")
    })
    @PostMapping("/{importId}/confirm")
    public ResponseEntity<PlaceConfirmResponse> confirm(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "회원별 장소 분석 작업 ID")
            @PathVariable Long importId,
            @Valid @RequestBody PlaceConfirmRequest request
    ) {
        List<PlaceCommandService.PlaceSelection> selections = request.selections().stream()
                .map(selection -> new PlaceCommandService.PlaceSelection(
                        selection.candidateId(),
                        selection.alias(),
                        selection.memo()
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
