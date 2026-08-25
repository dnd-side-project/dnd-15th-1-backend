package kr.omong.dulpick.domain.place.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.omong.dulpick.domain.place.application.OperationsAdminService;
import kr.omong.dulpick.domain.place.application.OperationsAdminView;
import kr.omong.dulpick.domain.place.application.ContentImageStorageService;
import kr.omong.dulpick.domain.place.domain.ContentPublicationStatus;
import kr.omong.dulpick.domain.place.domain.PlaceImportStatus;
import kr.omong.dulpick.domain.place.presentation.dto.request.UpdateContentPublicationStatusRequest;
import kr.omong.dulpick.domain.place.presentation.dto.request.ManualPlaceLinkRequest;
import kr.omong.dulpick.domain.place.presentation.dto.request.UpdateContentAdminRequest;
import kr.omong.dulpick.domain.place.presentation.dto.request.UpdateContentPlacesRequest;
import kr.omong.dulpick.domain.place.presentation.dto.request.UpdatePlaceAdminRequest;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @Operation(summary = "게시글 운영자 상세 조회")
    @GetMapping("/contents/{contentId:[0-9]+}")
    public ResponseEntity<OperationsAdminView.ContentDetail> contentDetail(
            @PathVariable Long contentId
    ) {
        return ResponseEntity.ok(adminService.contentDetail(contentId));
    }

    @Operation(summary = "게시글 제목·내용 수정")
    @PatchMapping("/contents/{contentId:[0-9]+}")
    public ResponseEntity<OperationsAdminView.ContentDetail> updateContent(
            @PathVariable Long contentId,
            @Valid @RequestBody UpdateContentAdminRequest request
    ) {
        return ResponseEntity.ok(adminService.updateContent(contentId, request));
    }

    @Operation(summary = "게시글 연결 장소 수정")
    @PatchMapping("/contents/{contentId:[0-9]+}/places")
    public ResponseEntity<OperationsAdminView.ContentDetail> updateContentPlaces(
            @PathVariable Long contentId,
            @Valid @RequestBody UpdateContentPlacesRequest request
    ) {
        return ResponseEntity.ok(adminService.updateContentPlaces(contentId, request));
    }

    @Operation(summary = "게시글 이미지 운영자 업로드")
    @PostMapping(value = "/contents/{contentId:[0-9]+}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OperationsAdminView.ContentDetail> uploadContentImage(
            @PathVariable Long contentId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean thumbnail
    ) throws java.io.IOException {
        return ResponseEntity.ok(adminService.uploadContentImage(
                contentId,
                file.getBytes(),
                parseContentType(file),
                thumbnail
        ));
    }

    @Operation(summary = "게시글 이미지 삭제")
    @DeleteMapping("/contents/{contentId:[0-9]+}/images/{imageKey}")
    public ResponseEntity<OperationsAdminView.ContentDetail> deleteContentImage(
            @PathVariable Long contentId,
            @PathVariable String imageKey
    ) {
        return ResponseEntity.ok(adminService.deleteContentImage(contentId, imageKey));
    }

    @Operation(summary = "운영자용 게시글 이미지 조회")
    @GetMapping("/contents/{contentId:[0-9]+}/images/{imageKey}/file")
    public ResponseEntity<byte[]> findContentImage(
            @PathVariable Long contentId,
            @PathVariable String imageKey
    ) {
        ContentImageStorageService.StoredImage image = adminService.contentImage(imageKey, contentId);
        return ResponseEntity.ok()
                .contentType(image.contentType())
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(image.bytes());
    }

    @Operation(summary = "장소 운영자 상세 조회")
    @GetMapping("/places/{placeId:[0-9]+}")
    public ResponseEntity<OperationsAdminView.PlaceDetail> placeDetail(
            @PathVariable Long placeId
    ) {
        return ResponseEntity.ok(adminService.placeDetail(placeId));
    }

    @Operation(summary = "장소 세부사항 수정")
    @PatchMapping("/places/{placeId:[0-9]+}")
    public ResponseEntity<OperationsAdminView.PlaceDetail> updatePlace(
            @PathVariable Long placeId,
            @Valid @RequestBody UpdatePlaceAdminRequest request
    ) {
        return ResponseEntity.ok(adminService.updatePlace(placeId, request));
    }

    @Operation(summary = "장소 이미지 운영자 업로드")
    @PostMapping(value = "/places/{placeId:[0-9]+}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OperationsAdminView.PlaceDetail> uploadPlaceImage(
            @PathVariable Long placeId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean thumbnail
    ) throws java.io.IOException {
        return ResponseEntity.ok(adminService.uploadPlaceImage(
                placeId,
                file.getBytes(),
                parseContentType(file),
                thumbnail
        ));
    }

    @Operation(summary = "장소 이미지 삭제")
    @DeleteMapping("/places/{placeId:[0-9]+}/images/{imageId:[0-9]+}")
    public ResponseEntity<OperationsAdminView.PlaceDetail> deletePlaceImage(
            @PathVariable Long placeId,
            @PathVariable Long imageId
    ) {
        return ResponseEntity.ok(adminService.deletePlaceImage(placeId, imageId));
    }

    @Operation(summary = "장소 검색")
    @GetMapping("/places/search")
    public ResponseEntity<OperationsAdminView.PlaceSearchPage> searchPlaces(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminService.searchPlaces(query, page, size));
    }

    @Operation(summary = "실패한 장소 추출에 장소를 수동 연결하고 공개 처리")
    @PostMapping("/place-imports/{importId:[0-9]+}/manual-place")
    public ResponseEntity<OperationsAdminView.ContentDetail> manuallyLinkPlace(
            @PathVariable Long importId,
            @Valid @RequestBody ManualPlaceLinkRequest request
    ) {
        return ResponseEntity.ok(adminService.manuallyLinkPlace(importId, request));
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

    private MediaType parseContentType(MultipartFile file) {
        try {
            return file.getContentType() == null
                    ? null
                    : MediaType.parseMediaType(file.getContentType());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
