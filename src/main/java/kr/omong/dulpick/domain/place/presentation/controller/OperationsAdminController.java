package kr.omong.dulpick.domain.place.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.omong.dulpick.domain.place.application.OperationsAdminService;
import kr.omong.dulpick.domain.place.application.OperationsAdminView;
import kr.omong.dulpick.domain.place.domain.ContentPublicationStatus;
import kr.omong.dulpick.domain.place.domain.PlaceImportStatus;
import kr.omong.dulpick.domain.place.presentation.dto.request.UpdateContentPublicationStatusRequest;
import kr.omong.dulpick.global.config.SwaggerTagNames;
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
@RequestMapping("/api/v1/admin")
public class OperationsAdminController {

    private final OperationsAdminService adminService;

    public OperationsAdminController(OperationsAdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "운영 대시보드 요약 조회")
    @GetMapping("/overview")
    public ResponseEntity<OperationsAdminView.Dashboard> overview() {
        return ResponseEntity.ok(adminService.dashboard());
    }

    @Operation(summary = "장소 추출 작업 목록 조회")
    @GetMapping("/place-imports")
    public ResponseEntity<OperationsAdminView.ImportPage> imports(
            @Parameter(example = "FAILED") @RequestParam(required = false) @Schema(example = "FAILED") PlaceImportStatus status,
            @Parameter(example = "PLACE_NOT_VERIFIED") @RequestParam(required = false) @Schema(example = "PLACE_NOT_VERIFIED") String failureCode,
            @Parameter(example = "instagram.com") @RequestParam(required = false) @Schema(example = "instagram.com") String query,
            @Parameter(example = "0") @RequestParam(defaultValue = "0") @Schema(example = "0") int page,
            @Parameter(example = "20") @RequestParam(defaultValue = "20") @Schema(example = "20") int size
    ) {
        return ResponseEntity.ok(adminService.imports(status, failureCode, query, page, size));
    }

    @Operation(summary = "장소 추출 작업 상세 조회")
    @GetMapping("/place-imports/{importId:[0-9]+}")
    public ResponseEntity<OperationsAdminView.ImportDetail> importDetail(
            @Parameter(example = "1001")
            @PathVariable @Schema(example = "1001") Long importId
    ) {
        return ResponseEntity.ok(adminService.importDetail(importId));
    }

    @Operation(summary = "장소 추출 작업 운영자 재처리")
    @PostMapping("/place-imports/{importId:[0-9]+}/retry")
    public ResponseEntity<Void> retryImport(
            @Parameter(example = "1001") @PathVariable @Schema(example = "1001") Long importId
    ) {
        adminService.retryImport(importId);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "콘텐츠 공개 상태 목록 조회")
    @GetMapping("/contents")
    public ResponseEntity<OperationsAdminView.ContentPage> contents(
            @Parameter(example = "PENDING") @RequestParam(required = false) @Schema(example = "PENDING") ContentPublicationStatus status,
            @Parameter(example = "데이트") @RequestParam(required = false) @Schema(example = "데이트") String query,
            @Parameter(example = "0") @RequestParam(defaultValue = "0") @Schema(example = "0") int page,
            @Parameter(example = "20") @RequestParam(defaultValue = "20") @Schema(example = "20") int size
    ) {
        return ResponseEntity.ok(adminService.contents(status, query, page, size));
    }

    @Operation(summary = "콘텐츠 공개 상태 변경")
    @PatchMapping("/contents/{contentId:[0-9]+}/publication-status")
    public ResponseEntity<OperationsAdminView.ContentSummary> updatePublicationStatus(
            @Parameter(example = "2001")
            @PathVariable @Schema(example = "2001") Long contentId,
            @Valid @RequestBody UpdateContentPublicationStatusRequest request
    ) {
        return ResponseEntity.ok(adminService.updatePublicationStatus(contentId, request));
    }

    @Operation(summary = "이미지 보강 백로그 조회")
    @GetMapping("/image-backlogs")
    public ResponseEntity<OperationsAdminView.ImageBacklogPage> imageBacklogs(
            @Parameter(example = "CONTENT") @RequestParam(defaultValue = "ALL") @Schema(example = "CONTENT") String kind,
            @Parameter(example = "0") @RequestParam(defaultValue = "0") @Schema(example = "0") int page,
            @Parameter(example = "20") @RequestParam(defaultValue = "20") @Schema(example = "20") int size
    ) {
        return ResponseEntity.ok(adminService.imageBacklogs(kind, page, size));
    }

    @Operation(summary = "게시글 이미지 재처리")
    @PostMapping("/content-images/{contentId:[0-9]+}/retry")
    public ResponseEntity<Void> retryContentImages(
            @Parameter(example = "2001") @PathVariable @Schema(example = "2001") Long contentId
    ) {
        adminService.retryContentImages(contentId);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "장소 이미지 재처리")
    @PostMapping("/place-images/{placeId:[0-9]+}/retry")
    public ResponseEntity<Void> retryPlaceImages(
            @Parameter(example = "101") @PathVariable @Schema(example = "101") Long placeId
    ) {
        adminService.retryPlaceImages(placeId);
        return ResponseEntity.accepted().build();
    }
}
