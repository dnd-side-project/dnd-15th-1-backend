package kr.omong.dulpick.domain.place.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.omong.dulpick.domain.place.application.PlaceQueryService;
import kr.omong.dulpick.domain.place.application.PlaceCommandService;
import kr.omong.dulpick.domain.place.application.PlaceSearchService;
import kr.omong.dulpick.domain.place.application.PlaceSearchResult;
import kr.omong.dulpick.domain.place.presentation.dto.request.ManualPlaceSaveRequest;
import kr.omong.dulpick.domain.place.presentation.dto.response.MemberPlaceResponse;
import kr.omong.dulpick.domain.place.presentation.dto.response.PlaceSearchResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = SwaggerTagNames.PLACE, description = "공용 장소와 커플 저장 장소 조회 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/places")
public class PlaceController {

    private final PlaceQueryService placeQueryService;
    private final PlaceSearchService placeSearchService;
    private final PlaceCommandService placeCommandService;

    public PlaceController(
            PlaceQueryService placeQueryService,
            PlaceSearchService placeSearchService,
            PlaceCommandService placeCommandService
    ) {
        this.placeQueryService = placeQueryService;
        this.placeSearchService = placeSearchService;
        this.placeCommandService = placeCommandService;
    }

    @Operation(
            summary = "내 장소와 연결된 상대방의 저장 장소 조회",
            description = "본인 저장 장소와 현재 연결된 상대방의 저장 장소를 조회합니다. 연결 해제 후 상대방 저장 정보는 반환하지 않습니다."
    )
    @GetMapping
    public ResponseEntity<List<MemberPlaceResponse>> getVisiblePlaces(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(placeQueryService.getVisiblePlaces(memberId(jwt))
                .stream()
                .map(MemberPlaceResponse::from)
                .toList());
    }

    @Operation(
            summary = "Kakao 장소 검색",
            description = "AI 분석과 무관하게 Kakao 지도에서 장소를 검색합니다. 검색 결과는 공용 장소 데이터로 정규화됩니다."
    )
    @GetMapping("/search")
    public ResponseEntity<List<PlaceSearchResponse>> search(
            @RequestParam String query
    ) {
        return ResponseEntity.ok(placeSearchService.search(query).stream()
                .map(PlaceSearchResponse::from)
                .toList());
    }

    @Operation(
            summary = "Kakao 검색 장소 저장",
            description = "Kakao 검색 결과의 장소를 개인 저장 목록에 추가하고 별칭을 설정합니다."
    )
    @PostMapping
    public ResponseEntity<MemberPlaceResponse> save(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ManualPlaceSaveRequest request
    ) {
        PlaceSearchResult searchResult = placeSearchService.resolve(
                request.query(),
                request.kakaoPlaceId()
        );
        return ResponseEntity.ok(MemberPlaceResponse.from(placeCommandService.saveManual(
                memberId(jwt),
                searchResult,
                request.alias(),
                request.memo()
        )));
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
