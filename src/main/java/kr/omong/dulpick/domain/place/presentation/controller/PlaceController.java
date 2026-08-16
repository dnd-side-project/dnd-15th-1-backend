package kr.omong.dulpick.domain.place.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import kr.omong.dulpick.global.exception.ErrorResponse;
import org.springframework.http.MediaType;
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
            description = "본인 저장 장소와 현재 연결된 상대방의 저장 장소를 조회합니다. 연결 해제 후 상대방 저장 정보는 반환하지 않습니다. "
                    + "같은 공용 장소를 둘 다 저장한 경우 한 항목으로 합쳐서 ownershipStatus=TOGETHER로 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "저장 장소 조회 성공. 장소가 없으면 빈 배열을 반환합니다.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = MemberPlaceResponse.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 없거나 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
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
            description = "AI 분석과 무관하게 Kakao 지도에서 장소를 검색합니다. 검색 결과는 공용 장소 데이터로 정규화됩니다. "
                    + "검색 결과의 kakaoPlaceId를 장소 저장 요청에 사용합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Kakao 장소 검색 성공. 결과가 없으면 빈 배열을 반환합니다.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = PlaceSearchResponse.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "검색어가 비어 있거나 허용 길이를 초과했습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Kakao 장소 검색에 실패했습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/search")
    public ResponseEntity<List<PlaceSearchResponse>> search(
            @Parameter(description = "Kakao 장소 검색어", required = true, example = "성수동 카페")
            @RequestParam String query
    ) {
        return ResponseEntity.ok(placeSearchService.search(query).stream()
                .map(PlaceSearchResponse::from)
                .toList());
    }

    @Operation(
            summary = "Kakao 검색 장소 저장",
            description = "Kakao 검색 결과의 장소를 개인 저장 목록에 추가하고 선택적으로 별칭을 설정합니다. "
                    + "memo 필드는 지원하지 않으며, alias를 생략하면 별칭 없이 저장합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "장소 저장 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MemberPlaceResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Kakao 장소 ID·검색어가 누락되었거나 alias 길이가 허용 범위를 벗어났습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 없거나 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "현재 회원이 이미 저장한 장소입니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
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
                request.alias()
        )));
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
