package kr.omong.dulpick.domain.date.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kr.omong.dulpick.domain.date.application.query.HomeQueryService;
import kr.omong.dulpick.domain.date.presentation.dto.response.DateCourseSummaryResponse;
import kr.omong.dulpick.domain.date.presentation.dto.response.HomeOverviewResponse;
import kr.omong.dulpick.domain.place.presentation.dto.response.MemberPlaceResponse;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = SwaggerTagNames.DATE, description = "홈 데이트 정보 조회 API")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequestMapping("/api/v1/home")
public class HomeController {

    private final HomeQueryService homeQueryService;

    public HomeController(HomeQueryService homeQueryService) {
        this.homeQueryService = homeQueryService;
    }

    @Operation(
            summary = "홈 개요 조회",
            description = """
                    홈 화면의 커플 닉네임과 현재 예정된 데이트를 조회합니다.
                    현재 예정된 데이트는 현재 시각 이후 확정(CONFIRMED) 일정 중 가장 가까운 한 건입니다.
                    """
    )
    @GetMapping
    public ResponseEntity<HomeOverviewResponse> overview(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(HomeOverviewResponse.from(
                homeQueryService.getOverview(memberId(jwt))
        ));
    }

    @Operation(
            summary = "홈 최근 저장 장소 미리보기",
            description = "size 파라미터 개수만큼 최근 저장 장소를 조회합니다."
    )
    @GetMapping("/recent-saved-places")
    public ResponseEntity<List<MemberPlaceResponse>> recentSavedPlaces(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @Min(1) @Max(50) int size
    ) {
        return ResponseEntity.ok(homeQueryService.getRecentSavedPlaces(memberId(jwt), size)
                .stream()
                .map(MemberPlaceResponse::from)
                .toList());
    }

    @Operation(
            summary = "홈 최근 저장 장소 전체보기",
            description = "최근 저장 장소 전체 목록을 최신순으로 조회합니다."
    )
    @GetMapping("/recent-saved-places/all")
    public ResponseEntity<List<MemberPlaceResponse>> allRecentSavedPlaces(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(homeQueryService.getRecentSavedPlacesAll(memberId(jwt))
                .stream()
                .map(MemberPlaceResponse::from)
                .toList());
    }

    @Operation(
            summary = "홈 지난 데이트 조회",
            description = "현재 시각 이전의 확정(CONFIRMED) 데이트를 최신순으로 조회합니다."
    )
    @GetMapping("/past-dates")
    public ResponseEntity<List<DateCourseSummaryResponse>> pastDates(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
    ) {
        return ResponseEntity.ok(homeQueryService.getPastDates(memberId(jwt), size)
                .stream()
                .map(DateCourseSummaryResponse::from)
                .toList());
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
