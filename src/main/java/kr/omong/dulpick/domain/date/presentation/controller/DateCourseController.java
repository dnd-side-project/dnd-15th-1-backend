package kr.omong.dulpick.domain.date.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kr.omong.dulpick.domain.date.application.command.DateCourseCommandService;
import kr.omong.dulpick.domain.date.application.query.DateCourseQueryService;
import kr.omong.dulpick.domain.date.presentation.dto.request.CreateDateCourseRequest;
import kr.omong.dulpick.domain.date.presentation.dto.request.SaveDateCourseRequest;
import kr.omong.dulpick.domain.date.presentation.dto.response.CurrentDateCourseResponse;
import kr.omong.dulpick.domain.date.presentation.dto.response.DateCoursePlacePoolResponse;
import kr.omong.dulpick.domain.date.presentation.dto.response.DateCourseResponse;
import kr.omong.dulpick.domain.date.presentation.dto.response.DateCourseSummaryResponse;
import kr.omong.dulpick.domain.place.domain.DulpickPlaceCategory;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = SwaggerTagNames.DATE, description = "데이트 코스 생성·수정·조회 API")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequestMapping("/api/v1/date-courses")
public class DateCourseController {

    private final DateCourseCommandService dateCourseCommandService;
    private final DateCourseQueryService dateCourseQueryService;

    public DateCourseController(
            DateCourseCommandService dateCourseCommandService,
            DateCourseQueryService dateCourseQueryService
    ) {
        this.dateCourseCommandService = dateCourseCommandService;
        this.dateCourseQueryService = dateCourseQueryService;
    }

    @Operation(
            summary = "데이트 코스 장소 선택용 커플 저장 장소 조회",
            description = """
                    현재 커플이 저장한 장소만 조회합니다.
                    region 문자열, category(일반 카테고리) 필터를 선택적으로 적용할 수 있습니다.
                    category는 RESTAURANT, CAFE, ENTERTAINMENT, SHOPPING, CONVENIENCE, TOURISM, ACCOMMODATION 값을 사용합니다.
                    """
    )
    @GetMapping("/places")
    public ResponseEntity<DateCoursePlacePoolResponse> getPlacePool(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "지역 필터(예: 성동구, 강남구)", example = "성동구")
            @RequestParam(required = false) @Schema(example = "성동구") String region,
            @Parameter(
                    description = "일반 카테고리 필터(RESTAURANT, CAFE, ENTERTAINMENT, "
                            + "SHOPPING, CONVENIENCE, TOURISM, ACCOMMODATION)",
                    example = "CAFE"
            )
            @RequestParam(required = false) @Schema(example = "CAFE") DulpickPlaceCategory category
    ) {
        return ResponseEntity.ok(DateCoursePlacePoolResponse.from(
                dateCourseQueryService.getCoupleSavedPlacePool(
                        memberId(jwt),
                        region,
                        category
                )
        ));
    }

    @Operation(
            summary = "데이트 코스 기본 정보 생성",
            description = "데이트명, 날짜, 시간을 입력해 임시(DRAFT) 데이트 코스를 생성합니다."
    )
    @PostMapping
    public ResponseEntity<DateCourseResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateDateCourseRequest request
    ) {
        return ResponseEntity.status(201).body(DateCourseResponse.from(
                dateCourseCommandService.create(memberId(jwt), request.toCommand())
        ));
    }

    @Operation(
            summary = "데이트 코스 임시 저장 또는 확정 저장",
            description = """
                    데이트명/날짜/시간/장소 순서를 한 번에 갱신합니다.
                    saveType은 TEMPORARY(임시 저장) 또는 CONFIRM(최종 저장)입니다.
                    낙관적 락을 위해 요청에 version을 포함해야 하며, 충돌 시 409를 반환합니다.
                    인접한 장소 간 도보 거리/시간은 Kakao 도보 경로 API로 조회해 places[].walkToNext에 포함합니다.
                    """
    )
    @PutMapping("/{dateCourseId}")
    public ResponseEntity<DateCourseResponse> save(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "데이트 코스 ID", required = true, example = "1001")
            @PathVariable @Schema(example = "1001") Long dateCourseId,
            @Valid @RequestBody SaveDateCourseRequest request
    ) {
        return ResponseEntity.ok(DateCourseResponse.from(
                dateCourseCommandService.save(memberId(jwt), dateCourseId, request.toCommand())
        ));
    }

    @Operation(
            summary = "데이트 코스 상세 조회",
            description = "저장된 데이트 코스의 기본 정보, 장소 순서, 인접 장소 간 도보 이동거리/시간을 조회합니다."
    )
    @GetMapping("/{dateCourseId}")
    public ResponseEntity<DateCourseResponse> get(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "데이트 코스 ID", required = true, example = "1001")
            @PathVariable @Schema(example = "1001") Long dateCourseId
    ) {
        return ResponseEntity.ok(DateCourseResponse.from(
                dateCourseQueryService.getDateCourse(memberId(jwt), dateCourseId)
        ));
    }

    @Operation(
            summary = "현재 예정된 데이트 조회",
            description = "현재 시각 이후 확정(CONFIRMED) 일정 중 가장 가까운 한 건을 조회합니다."
    )
    @GetMapping("/current")
    public ResponseEntity<CurrentDateCourseResponse> current(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(CurrentDateCourseResponse.from(
                dateCourseQueryService.getCurrentUpcomingConfirmed(memberId(jwt))
        ));
    }

    @Operation(
            summary = "지난 데이트 목록 조회",
            description = "현재 시각 이전 확정(CONFIRMED) 데이트를 최신순으로 조회합니다."
    )
    @GetMapping("/past")
    public ResponseEntity<List<DateCourseSummaryResponse>> past(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "조회할 지난 데이트 수(기본값 20, 최소 1, 최대 50)", example = "20")
            @RequestParam(defaultValue = "20") @Schema(example = "20") @Min(1) @Max(50) int size
    ) {
        return ResponseEntity.ok(dateCourseQueryService.getPastConfirmed(memberId(jwt), size)
                .stream()
                .map(DateCourseSummaryResponse::from)
                .toList());
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
