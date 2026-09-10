package kr.omong.dulpick.domain.place.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.omong.dulpick.domain.analytics.application.AnalyticsMetricsService;
import kr.omong.dulpick.domain.analytics.presentation.AnalyticsMetricsResponse;
import kr.omong.dulpick.domain.analytics.presentation.AnalyticsComparisonResponse;
import kr.omong.dulpick.domain.analytics.presentation.AnalyticsFunnelResponse;
import kr.omong.dulpick.domain.analytics.presentation.AnalyticsRetentionResponse;
import kr.omong.dulpick.domain.analytics.presentation.AnalyticsTrendResponse;
import kr.omong.dulpick.domain.analytics.application.InvalidMetricPeriodException;
import kr.omong.dulpick.domain.place.application.OperationsAdminService;
import kr.omong.dulpick.domain.place.application.OperationsAdminView;
import kr.omong.dulpick.domain.place.application.ContentImageStorageService;
import kr.omong.dulpick.domain.place.domain.ContentPublicationStatus;
import kr.omong.dulpick.domain.place.domain.PlaceImportStatus;
import kr.omong.dulpick.domain.place.presentation.dto.request.UpdateContentPublicationStatusRequest;
import kr.omong.dulpick.domain.place.presentation.dto.request.CreateAdminPlaceRequest;
import kr.omong.dulpick.domain.place.presentation.dto.request.ManualPlaceLinkRequest;
import kr.omong.dulpick.domain.place.presentation.dto.request.CompletePlaceImportRequest;
import kr.omong.dulpick.domain.place.presentation.dto.request.ReviewPlaceCandidateRequest;
import kr.omong.dulpick.domain.place.presentation.dto.request.UpdateContentAdminRequest;
import kr.omong.dulpick.domain.place.presentation.dto.request.UpdateContentPlacesRequest;
import kr.omong.dulpick.domain.place.presentation.dto.request.UpdatePlaceAdminRequest;
import kr.omong.dulpick.domain.place.presentation.dto.request.ReorderContentImagesRequest;
import kr.omong.dulpick.domain.place.presentation.dto.request.ReorderPlaceImagesRequest;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Tag(name = SwaggerTagNames.OPS, description = "운영자 대시보드·장애 대응 API")
@SecurityRequirement(name = "basicAuth")
@RestController
@RequestMapping("/api/v1/admin")
public class OperationsAdminController {

    private static final int MAX_METRIC_PERIOD_DAYS = 366;

    private final OperationsAdminService adminService;
    private final AnalyticsMetricsService analyticsMetricsService;

    public OperationsAdminController(
            OperationsAdminService adminService,
            AnalyticsMetricsService analyticsMetricsService
    ) {
        this.adminService = adminService;
        this.analyticsMetricsService = analyticsMetricsService;
    }

    @Operation(summary = "운영 대시보드 요약 조회")
    @GetMapping("/overview")
    public ResponseEntity<OperationsAdminView.Dashboard> overview() {
        return ResponseEntity.ok(adminService.dashboard());
    }

    @Operation(summary = "성과 지표 요약 조회")
    @GetMapping("/metrics/overview")
    public ResponseEntity<AnalyticsMetricsResponse> metricsOverview(
            @Parameter(description = "조회 시작일(포함). 생략하면 이번 달 1일입니다.", example = "2026-09-01")
            @RequestParam(required = false) LocalDate from,
            @Parameter(description = "조회 종료일(미포함). 생략하면 다음 달 1일입니다.", example = "2026-10-01")
            @RequestParam(required = false) LocalDate to
    ) {
        MetricPeriod period = metricPeriod(from, to);
        return ResponseEntity.ok(AnalyticsMetricsResponse.from(
                analyticsMetricsService.overview(period.from(), period.to())
        ));
    }

    @Operation(summary = "성과 지표 기간 비교 조회")
    @GetMapping("/metrics/comparison")
    public ResponseEntity<AnalyticsComparisonResponse> metricsComparison(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate startDate = from == null ? today.withDayOfMonth(1) : from;
        LocalDate endDate = to == null ? startDate.plusMonths(1) : to;
        MetricPeriod current = metricPeriod(startDate, endDate);
        long periodDays = ChronoUnit.DAYS.between(startDate, endDate);
        MetricPeriod previous = metricPeriod(
                startDate.minusDays(periodDays),
                startDate
        );
        return ResponseEntity.ok(AnalyticsComparisonResponse.from(
                analyticsMetricsService.overview(current.from(), current.to()),
                analyticsMetricsService.overview(previous.from(), previous.to())
        ));
    }

    @Operation(summary = "성과 지표 일별 추이 조회")
    @GetMapping("/metrics/trends")
    public ResponseEntity<AnalyticsTrendResponse> metricsTrends(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        MetricPeriod period = metricPeriod(from, to);
        return ResponseEntity.ok(AnalyticsTrendResponse.from(
                analyticsMetricsService.trends(period.from(), period.to())
        ));
    }

    @Operation(summary = "성과 지표 퍼널 조회")
    @GetMapping("/metrics/funnel")
    public ResponseEntity<AnalyticsFunnelResponse> metricsFunnel(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        MetricPeriod period = metricPeriod(from, to);
        return ResponseEntity.ok(AnalyticsFunnelResponse.from(
                analyticsMetricsService.funnel(period.from(), period.to())
        ));
    }

    @Operation(summary = "성과 지표 리텐션 조회")
    @GetMapping("/metrics/retention")
    public ResponseEntity<AnalyticsRetentionResponse> metricsRetention(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        MetricPeriod period = metricPeriod(from, to);
        return ResponseEntity.ok(AnalyticsRetentionResponse.from(
                analyticsMetricsService.retention(period.from(), period.to())
        ));
    }

    private MetricPeriod metricPeriod(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate startDate = from == null ? today.withDayOfMonth(1) : from;
        LocalDate endDate = to == null ? startDate.plusMonths(1) : to;
        if (!startDate.isBefore(endDate)
                || startDate.plusDays(MAX_METRIC_PERIOD_DAYS).isBefore(endDate)) {
            throw new InvalidMetricPeriodException();
        }
        ZoneId zone = ZoneId.of("Asia/Seoul");
        return new MetricPeriod(
                startDate.atStartOfDay(zone).toInstant(),
                endDate.atStartOfDay(zone).toInstant()
        );
    }

    private record MetricPeriod(Instant from, Instant to) {
    }

    @Operation(
            summary = "일별 장소 추출 통계 조회",
            description = "최근 N일(7~30, 기본 14)의 요청 수·완료·후보 확인·실패와 평균 처리 시간을 반환합니다."
    )
    @GetMapping("/stats/daily")
    public ResponseEntity<OperationsAdminView.DailyStats> dailyStats(
            @Parameter(example = "14") @RequestParam(defaultValue = "14") @Schema(example = "14") int days
    ) {
        return ResponseEntity.ok(adminService.dailyStats(days));
    }

    @Operation(
            summary = "장소 추출 작업 목록 조회",
            description = "hasUnverified=true이면 검증되지 않은 후보(EXTRACTED)가 남아 있는 작업만 조회합니다."
    )
    @GetMapping("/place-imports")
    public ResponseEntity<OperationsAdminView.ImportPage> imports(
            @Parameter(example = "FAILED") @RequestParam(required = false) @Schema(example = "FAILED") PlaceImportStatus status,
            @Parameter(example = "PLACE_NOT_VERIFIED") @RequestParam(required = false) @Schema(example = "PLACE_NOT_VERIFIED") String failureCode,
            @Parameter(example = "instagram.com") @RequestParam(required = false) @Schema(example = "instagram.com") String query,
            @Parameter(description = "미검증 후보가 남은 작업만 조회") @RequestParam(required = false) @Schema(example = "true") Boolean hasUnverified,
            @Parameter(example = "0") @RequestParam(defaultValue = "0") @Schema(example = "0") int page,
            @Parameter(example = "20") @RequestParam(defaultValue = "20") @Schema(example = "20") int size
    ) {
        return ResponseEntity.ok(adminService.imports(
                status, failureCode, query, Boolean.TRUE.equals(hasUnverified), page, size
        ));
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
            @Parameter(example = "2001") @PathVariable @Schema(example = "2001") Long contentId
    ) {
        return ResponseEntity.ok(adminService.contentDetail(contentId));
    }

    @Operation(summary = "게시글 제목·내용 수정")
    @PatchMapping("/contents/{contentId:[0-9]+}")
    public ResponseEntity<OperationsAdminView.ContentDetail> updateContent(
            @Parameter(example = "2001") @PathVariable @Schema(example = "2001") Long contentId,
            @Valid @RequestBody UpdateContentAdminRequest request
    ) {
        return ResponseEntity.ok(adminService.updateContent(contentId, request));
    }

    @Operation(summary = "게시글 연결 장소 수정", description = "장소를 저장하고 publish=true이면 게시글을 PUBLIC으로 전환합니다.")
    @PatchMapping("/contents/{contentId:[0-9]+}/places")
    public ResponseEntity<OperationsAdminView.ContentDetail> updateContentPlaces(
            @Parameter(example = "2001") @PathVariable @Schema(example = "2001") Long contentId,
            @Valid @RequestBody UpdateContentPlacesRequest request
    ) {
        return ResponseEntity.ok(adminService.updateContentPlaces(contentId, request));
    }

    @Operation(summary = "게시글 이미지 운영자 업로드")
    @PostMapping(value = "/contents/{contentId:[0-9]+}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OperationsAdminView.ContentDetail> uploadContentImage(
            @Parameter(example = "2001") @PathVariable @Schema(example = "2001") Long contentId,
            @RequestPart("file") MultipartFile file,
            @Parameter(example = "true") @RequestParam(defaultValue = "false") @Schema(example = "true") boolean thumbnail,
            @Parameter(example = "2026-08-24T10:00:05Z") @RequestParam @Schema(example = "2026-08-24T10:00:05Z") Instant expectedUpdatedAt
    ) throws java.io.IOException {
        return ResponseEntity.ok(adminService.uploadContentImage(
                contentId,
                file.getBytes(),
                parseContentType(file),
                thumbnail,
                expectedUpdatedAt
        ));
    }

    @Operation(summary = "게시글 이미지 삭제")
    @DeleteMapping("/contents/{contentId:[0-9]+}/images/{imageKey}")
    public ResponseEntity<OperationsAdminView.ContentDetail> deleteContentImage(
            @Parameter(example = "2001") @PathVariable @Schema(example = "2001") Long contentId,
            @Parameter(example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable @Schema(example = "550e8400-e29b-41d4-a716-446655440000") String imageKey,
            @Parameter(example = "2026-08-24T10:00:05Z") @RequestParam @Schema(example = "2026-08-24T10:00:05Z") Instant expectedUpdatedAt
    ) {
        return ResponseEntity.ok(adminService.deleteContentImage(contentId, imageKey, expectedUpdatedAt));
    }

    @Operation(summary = "게시글 이미지 순서 변경")
    @PatchMapping("/contents/{contentId:[0-9]+}/images/order")
    public ResponseEntity<OperationsAdminView.ContentDetail> reorderContentImages(
            @Parameter(example = "2001") @PathVariable @Schema(example = "2001") Long contentId,
            @Valid @RequestBody ReorderContentImagesRequest request
    ) {
        return ResponseEntity.ok(adminService.reorderContentImages(contentId, request));
    }

    @Operation(summary = "게시글 대표 이미지 지정")
    @PatchMapping("/contents/{contentId:[0-9]+}/images/{imageKey}/thumbnail")
    public ResponseEntity<OperationsAdminView.ContentDetail> setContentThumbnail(
            @Parameter(example = "2001") @PathVariable @Schema(example = "2001") Long contentId,
            @Parameter(example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable @Schema(example = "550e8400-e29b-41d4-a716-446655440000") String imageKey,
            @Parameter(example = "2026-08-24T10:00:05Z") @RequestParam @Schema(example = "2026-08-24T10:00:05Z") Instant expectedUpdatedAt
    ) {
        return ResponseEntity.ok(adminService.setContentThumbnail(contentId, imageKey, expectedUpdatedAt));
    }

    @Operation(summary = "운영자용 게시글 이미지 조회")
    @GetMapping("/contents/{contentId:[0-9]+}/images/{imageKey}/file")
    public ResponseEntity<byte[]> findContentImage(
            @Parameter(example = "2001") @PathVariable @Schema(example = "2001") Long contentId,
            @Parameter(example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable @Schema(example = "550e8400-e29b-41d4-a716-446655440000") String imageKey
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
            @Parameter(example = "101") @PathVariable @Schema(example = "101") Long placeId
    ) {
        return ResponseEntity.ok(adminService.placeDetail(placeId));
    }

    @Operation(summary = "장소 세부사항 수정")
    @PatchMapping("/places/{placeId:[0-9]+}")
    public ResponseEntity<OperationsAdminView.PlaceDetail> updatePlace(
            @Parameter(example = "101") @PathVariable @Schema(example = "101") Long placeId,
            @Valid @RequestBody UpdatePlaceAdminRequest request
    ) {
        return ResponseEntity.ok(adminService.updatePlace(placeId, request));
    }

    @Operation(summary = "장소 이미지 운영자 업로드")
    @PostMapping(value = "/places/{placeId:[0-9]+}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OperationsAdminView.PlaceDetail> uploadPlaceImage(
            @Parameter(example = "101") @PathVariable @Schema(example = "101") Long placeId,
            @RequestPart("file") MultipartFile file,
            @Parameter(example = "true") @RequestParam(defaultValue = "false") @Schema(example = "true") boolean thumbnail,
            @Parameter(example = "2026-08-24T10:00:05Z") @RequestParam @Schema(example = "2026-08-24T10:00:05Z") Instant expectedUpdatedAt
    ) throws java.io.IOException {
        return ResponseEntity.ok(adminService.uploadPlaceImage(
                placeId,
                file.getBytes(),
                parseContentType(file),
                thumbnail,
                expectedUpdatedAt
        ));
    }

    @Operation(summary = "장소 이미지 삭제")
    @DeleteMapping("/places/{placeId:[0-9]+}/images/{imageId:[0-9]+}")
    public ResponseEntity<OperationsAdminView.PlaceDetail> deletePlaceImage(
            @Parameter(example = "101") @PathVariable @Schema(example = "101") Long placeId,
            @Parameter(example = "501") @PathVariable @Schema(example = "501") Long imageId,
            @Parameter(example = "2026-08-24T10:00:05Z") @RequestParam @Schema(example = "2026-08-24T10:00:05Z") Instant expectedUpdatedAt
    ) {
        return ResponseEntity.ok(adminService.deletePlaceImage(placeId, imageId, expectedUpdatedAt));
    }

    @Operation(summary = "장소 이미지 순서 변경")
    @PatchMapping("/places/{placeId:[0-9]+}/images/order")
    public ResponseEntity<OperationsAdminView.PlaceDetail> reorderPlaceImages(
            @Parameter(example = "101") @PathVariable @Schema(example = "101") Long placeId,
            @Valid @RequestBody ReorderPlaceImagesRequest request
    ) {
        return ResponseEntity.ok(adminService.reorderPlaceImages(placeId, request));
    }

    @Operation(summary = "장소 대표 이미지 지정")
    @PatchMapping("/places/{placeId:[0-9]+}/images/{imageId:[0-9]+}/thumbnail")
    public ResponseEntity<OperationsAdminView.PlaceDetail> setPlaceThumbnail(
            @Parameter(example = "101") @PathVariable @Schema(example = "101") Long placeId,
            @Parameter(example = "501") @PathVariable @Schema(example = "501") Long imageId,
            @Parameter(example = "2026-08-24T10:00:05Z") @RequestParam @Schema(example = "2026-08-24T10:00:05Z") Instant expectedUpdatedAt
    ) {
        return ResponseEntity.ok(adminService.setPlaceThumbnail(placeId, imageId, expectedUpdatedAt));
    }

    @Operation(summary = "장소 검색")
    @GetMapping("/places/search")
    public ResponseEntity<OperationsAdminView.PlaceSearchPage> searchPlaces(
            @Parameter(example = "카페") @RequestParam(defaultValue = "") @Schema(example = "카페") String query,
            @Parameter(example = "0") @RequestParam(defaultValue = "0") @Schema(example = "0") int page,
            @Parameter(example = "20") @RequestParam(defaultValue = "20") @Schema(example = "20") int size
    ) {
        return ResponseEntity.ok(adminService.searchPlaces(query, page, size));
    }

    @Operation(summary = "카카오맵 장소 검색")
    @GetMapping("/places/kakao-search")
    public ResponseEntity<OperationsAdminView.KakaoPlaceSearchPage> searchKakaoPlaces(
            @Parameter(example = "도원반점")
            @RequestParam @Schema(example = "도원반점") String query
    ) {
        return ResponseEntity.ok(adminService.searchKakaoPlaces(query));
    }

    @Operation(summary = "Kakao 장소 카테고리 그룹 코드 목록 조회")
    @GetMapping("/places/category-groups")
    public ResponseEntity<List<OperationsAdminView.PlaceCategoryGroupOption>> placeCategoryGroups() {
        return ResponseEntity.ok(adminService.placeCategoryGroups());
    }

    @Operation(
            summary = "신규 장소 등록",
            description = "카카오 장소 ID 기준으로 장소를 등록합니다. 이미 존재하면 기존 장소를 반환합니다. "
                    + "반환된 placeId로 수동 연결 API를 호출해 게시글에 추가할 수 있습니다."
    )
    @PostMapping("/places")
    public ResponseEntity<OperationsAdminView.PlaceSummary> createPlace(
            @Valid @RequestBody CreateAdminPlaceRequest request
    ) {
        return ResponseEntity.ok(adminService.createPlace(request));
    }

    @Operation(summary = "실패한 장소 추출에 장소를 수동 연결하고 공개 처리")
    @PostMapping("/place-imports/{importId:[0-9]+}/manual-place")
    public ResponseEntity<OperationsAdminView.ContentDetail> manuallyLinkPlace(
            @Parameter(example = "1001") @PathVariable @Schema(example = "1001") Long importId,
            @Valid @RequestBody ManualPlaceLinkRequest request
    ) {
        return ResponseEntity.ok(adminService.manuallyLinkPlace(importId, request));
    }

    @Operation(summary = "장소 추출 후보 제외")
    @DeleteMapping("/place-imports/{importId:[0-9]+}/candidates/{candidateId:[0-9]+}")
    public ResponseEntity<OperationsAdminView.ImportDetail> rejectPlaceCandidate(
            @Parameter(example = "1001") @PathVariable @Schema(example = "1001") Long importId,
            @Parameter(example = "3001") @PathVariable @Schema(example = "3001") Long candidateId,
            @Valid @RequestBody ReviewPlaceCandidateRequest request
    ) {
        return ResponseEntity.ok(adminService.rejectCandidate(
                importId, candidateId, request.expectedUpdatedAt()
        ));
    }

    @Operation(summary = "장소 추출 작업 최종 확정 및 공개")
    @PostMapping("/place-imports/{importId:[0-9]+}/complete")
    public ResponseEntity<OperationsAdminView.ContentDetail> completePlaceImport(
            @Parameter(example = "1001") @PathVariable @Schema(example = "1001") Long importId,
            @Valid @RequestBody CompletePlaceImportRequest request
    ) {
        return ResponseEntity.ok(adminService.completeManualPlaceImport(
                importId, request.expectedUpdatedAt()
        ));
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

    @Operation(summary = "장소 원본 이미지 재추출")
    @PostMapping("/places/{placeId:[0-9]+}/images/refresh")
    public ResponseEntity<Void> refreshPlaceImages(
            @Parameter(example = "101") @PathVariable @Schema(example = "101") Long placeId
    ) {
        adminService.refreshPlaceImages(placeId);
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
