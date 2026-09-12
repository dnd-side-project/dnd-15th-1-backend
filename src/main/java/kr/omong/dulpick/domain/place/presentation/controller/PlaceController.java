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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kr.omong.dulpick.domain.place.application.PlaceQueryService;
import kr.omong.dulpick.domain.place.application.PlaceCommandService;
import kr.omong.dulpick.domain.place.application.PlaceDetailQueryService;
import kr.omong.dulpick.domain.place.application.PlaceSearchService;
import kr.omong.dulpick.domain.place.application.PlaceSearchResult;
import kr.omong.dulpick.domain.place.application.PublicContentQueryService;
import kr.omong.dulpick.domain.place.config.ContentThumbnailProperties;
import kr.omong.dulpick.domain.place.domain.DulpickPlaceCategory;
import kr.omong.dulpick.domain.place.domain.PlaceOwnershipStatus;
import kr.omong.dulpick.domain.place.presentation.dto.request.ManualPlaceSaveRequest;
import kr.omong.dulpick.domain.place.presentation.dto.request.UpdatePlaceAliasRequest;
import kr.omong.dulpick.domain.place.presentation.dto.response.MemberPlaceResponse;
import kr.omong.dulpick.domain.place.presentation.dto.response.PlaceDetailResponse;
import kr.omong.dulpick.domain.place.presentation.dto.response.PlaceSaveDeleteResponse;
import kr.omong.dulpick.domain.place.presentation.dto.response.PlaceSearchPageResponse;
import kr.omong.dulpick.domain.place.presentation.dto.response.PublicContentPageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import kr.omong.dulpick.global.exception.ErrorResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = SwaggerTagNames.PLACE, description = "공용 장소와 커플 저장 장소 조회 API")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequestMapping("/api/v1/places")
public class PlaceController {

    private final PlaceQueryService placeQueryService;
    private final PlaceSearchService placeSearchService;
    private final PlaceDetailQueryService placeDetailQueryService;
    private final PlaceCommandService placeCommandService;
    private final PublicContentQueryService publicContentQueryService;
    private final ContentThumbnailProperties thumbnailProperties;

    public PlaceController(
            PlaceQueryService placeQueryService,
            PlaceSearchService placeSearchService,
            PlaceDetailQueryService placeDetailQueryService,
            PlaceCommandService placeCommandService,
            PublicContentQueryService publicContentQueryService,
            ContentThumbnailProperties thumbnailProperties
    ) {
        this.placeQueryService = placeQueryService;
        this.placeSearchService = placeSearchService;
        this.placeDetailQueryService = placeDetailQueryService;
        this.placeCommandService = placeCommandService;
        this.publicContentQueryService = publicContentQueryService;
        this.thumbnailProperties = thumbnailProperties;
    }

    @Operation(
            summary = "내 장소와 연결된 상대방의 저장 장소 조회",
            description = "본인 저장 장소와 현재 연결된 상대방의 저장 장소를 조회합니다. 연결 해제 후 상대방 저장 정보는 반환하지 않습니다. "
                    + "같은 공용 장소를 커플이 저장한 경우 ownershipStatus=TOGETHER로 반환합니다. "
                    + "카테고리·저장 주체 필터를 선택적으로 적용할 수 있습니다."
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
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "둘픽 카테고리 코드", example = "CAFE")
            @RequestParam(required = false) @Schema(example = "CAFE") DulpickPlaceCategory category,
            @Parameter(description = "저장 주체 필터", example = "TOGETHER")
            @RequestParam(required = false) @Schema(example = "TOGETHER")
            PlaceOwnershipStatus ownershipStatus
    ) {
        return ResponseEntity.ok(placeQueryService.getVisiblePlaces(
                        memberId(jwt),
                        category,
                        ownershipStatus
                )
                .stream()
                .map(MemberPlaceResponse::from)
                .toList());
    }

    @Operation(
            summary = "DB 우선 Kakao 장소 통합 검색",
            description = "공용 DB와 Kakao 지도에서 장소를 함께 검색하고 kakaoPlaceId로 중복 제거합니다. "
                    + "Kakao에는 검색어만 보내며, 카테고리는 응답의 Kakao 코드로 매핑합니다. "
                    + "한 번에 10건을 반환하고 page로 다음 10건을 요청합니다. "
                    + "첫 페이지는 DB 장소를 앞에 두고, 조회 중 검색 결과를 DB에 저장하지 않습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Kakao 장소 검색 성공. 결과가 없으면 빈 목록을 반환합니다.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PlaceSearchPageResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "검색어가 비어 있거나 허용 길이를 초과했습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 없거나 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Kakao 장소 검색을 일시적으로 사용할 수 없습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/search")
    public ResponseEntity<PlaceSearchPageResponse> search(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Kakao 장소 검색어", required = true, example = "성수동 카페")
            @RequestParam @NotBlank @Size(max = 200)
            @Schema(example = "성수동 카페") String query,
            @Parameter(description = "0부터 시작하는 페이지 번호. 다음 10건이 필요하면 1, 2, … 로 요청합니다.", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) @Max(44)
            @Schema(example = "0") int page
    ) {
        return ResponseEntity.ok(PlaceSearchPageResponse.from(placeSearchService.search(
                memberId(jwt),
                query,
                page
        )));
    }

    @Operation(
            summary = "Kakao 검색 장소 상세 조회",
            description = "검색어와 Kakao 장소 ID를 다시 검증해 미저장 장소 상세를 조회합니다. "
                    + "동일 Kakao 장소가 공용 DB에 있으면 DB 장소 ID와 현재 활성 커플의 저장 상태를 함께 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Kakao 장소 상세 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PlaceDetailResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Kakao 장소 ID나 검색어가 올바르지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 없거나 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "검색 결과에서 Kakao 장소를 찾을 수 없습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Kakao 장소 검색을 일시적으로 사용할 수 없습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/kakao/{kakaoPlaceId}")
    public ResponseEntity<PlaceDetailResponse> getKakaoPlaceDetail(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Kakao 장소 고유 ID", required = true, example = "18699959")
            @PathVariable @NotBlank @Size(max = 80)
            @Schema(example = "18699959") String kakaoPlaceId,
            @Parameter(description = "Kakao 장소를 확인할 검색어", required = true, example = "성수동 카페")
            @RequestParam @NotBlank @Size(max = 200)
            @Schema(example = "성수동 카페") String query
    ) {
        return ResponseEntity.ok(PlaceDetailResponse.from(
                placeDetailQueryService.getByKakaoPlaceId(
                        memberId(jwt),
                        kakaoPlaceId,
                        query
                )
        ));
    }

    @Operation(
            summary = "장소에 연결된 공개 게시물 조회",
            description = "해당 장소가 포함된 공개 게시물을 최신순으로 조회합니다. "
                    + "연결된 게시물이 없으면 빈 배열을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "장소에 연결된 공개 게시물 목록 조회 성공. 결과가 없으면 빈 배열을 반환합니다.",
                    content = @Content(schema = @Schema(implementation = PublicContentPageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 없거나 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "공용 장소를 찾을 수 없습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{placeId}/contents")
    public ResponseEntity<PublicContentPageResponse> findPublicContentsByPlaceId(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "공용 장소 ID", required = true, example = "101")
            @PathVariable @Schema(example = "101") Long placeId,
            @Parameter(description = "0부터 시작하는 페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) @Schema(example = "0") int page,
            @Parameter(description = "페이지당 게시물 수. 최대 50입니다.", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) @Schema(example = "20") int size
    ) {
        return ResponseEntity.ok(PublicContentPageResponse.from(
                publicContentQueryService.findPublicContentsByPlaceId(
                        memberId(jwt),
                        placeId,
                        PageRequest.of(page, size)
                ),
                thumbnailProperties.baseUrl()
        ));
    }

    @Operation(
            summary = "공용 DB 장소 상세 조회",
            description = "공용 장소 정보와 현재 활성 커플 기준 저장 여부·저장 주체를 함께 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "장소 상세 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PlaceDetailResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 없거나 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "공용 장소를 찾을 수 없습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{placeId}")
    public ResponseEntity<PlaceDetailResponse> getPlaceDetail(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "공용 장소 ID", required = true, example = "101")
            @PathVariable @Schema(example = "101") Long placeId
    ) {
        return ResponseEntity.ok(PlaceDetailResponse.from(
                placeDetailQueryService.get(memberId(jwt), placeId)
        ));
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

    @Operation(
            summary = "저장한 장소 별칭 수정",
            description = "현재 회원이 저장한 장소의 alias를 수정합니다. 공용 장소 정보는 변경하지 않습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "별칭 수정 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MemberPlaceResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "alias 길이가 허용 범위를 벗어났습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 없거나 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "공용 장소가 없거나 현재 회원이 저장하지 않은 장소입니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PatchMapping("/{placeId}/alias")
    public ResponseEntity<MemberPlaceResponse> updateAlias(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "공용 장소 ID", required = true, example = "101")
            @PathVariable @Schema(example = "101") Long placeId,
            @Valid @RequestBody UpdatePlaceAliasRequest request
    ) {
        return ResponseEntity.ok(MemberPlaceResponse.from(placeCommandService.updateAlias(
                memberId(jwt),
                placeId,
                request.alias()
        )));
    }

    @Operation(
            summary = "저장한 장소 삭제",
            description = "현재 회원과 공용 장소의 저장 관계만 삭제합니다. 공용 장소 데이터는 유지됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "저장 관계 삭제 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PlaceSaveDeleteResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 없거나 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "공용 장소가 없거나 현재 회원이 저장하지 않은 장소입니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{placeId}")
    public ResponseEntity<PlaceSaveDeleteResponse> deleteSave(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "저장을 해제할 공용 장소 ID", required = true, example = "101")
            @PathVariable @Schema(example = "101") Long placeId
    ) {
        PlaceCommandService.PlaceSaveDeleted deleted = placeCommandService.deleteSave(
                memberId(jwt),
                placeId
        );
        return ResponseEntity.ok(new PlaceSaveDeleteResponse(deleted.deleted(), deleted.placeId()));
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
