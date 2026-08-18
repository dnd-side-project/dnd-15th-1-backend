package kr.omong.dulpick.domain.search.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.omong.dulpick.domain.search.application.RecentSearchCommandService;
import kr.omong.dulpick.domain.search.application.RecentSearchQueryService;
import kr.omong.dulpick.domain.search.domain.RecentSearchType;
import kr.omong.dulpick.domain.search.presentation.dto.request.SaveRecentSearchRequest;
import kr.omong.dulpick.domain.search.presentation.dto.response.RecentSearchPageResponse;
import kr.omong.dulpick.domain.search.presentation.dto.response.RecentSearchResponse;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import kr.omong.dulpick.global.exception.ErrorResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = SwaggerTagNames.SEARCH, description = "게시글·장소 검색 도메인별 최근 검색어 API")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequestMapping("/api/v1/recent-searches")
public class RecentSearchController {

    private final RecentSearchCommandService recentSearchCommandService;
    private final RecentSearchQueryService recentSearchQueryService;

    public RecentSearchController(
            RecentSearchCommandService recentSearchCommandService,
            RecentSearchQueryService recentSearchQueryService
    ) {
        this.recentSearchCommandService = recentSearchCommandService;
        this.recentSearchQueryService = recentSearchQueryService;
    }

    @Operation(
            summary = "최근 검색어 저장",
            description = "게시글과 장소 검색어를 타입별로 저장합니다. 같은 타입에서 동일 검색어를 다시 저장하면 최신 시각으로 이동합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "최근 검색어 저장 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RecentSearchResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "검색 타입이나 검색어가 올바르지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 없거나 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<RecentSearchResponse> record(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SaveRecentSearchRequest request
    ) {
        return ResponseEntity.status(201).body(RecentSearchResponse.from(
                recentSearchCommandService.record(
                        memberId(jwt),
                        request.type(),
                        request.keyword()
                )
        ));
    }

    @Operation(
            summary = "최근 검색어 조회",
            description = "CONTENT 또는 PLACE 검색어를 마지막 검색 시각 역순으로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "최근 검색어 조회 성공",
                    content = @Content(schema = @Schema(implementation = RecentSearchPageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "검색 타입이나 페이지 값이 올바르지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 없거나 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<RecentSearchPageResponse> getRecentSearches(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "조회할 검색 도메인", required = true, example = "PLACE")
            @RequestParam @Schema(example = "PLACE") RecentSearchType type,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(RecentSearchPageResponse.from(
                recentSearchQueryService.getRecentSearches(memberId(jwt), type, pageable)
        ));
    }

    @Operation(summary = "최근 검색어 한 건 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "최근 검색어 삭제 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 없거나 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "내 최근 검색어를 찾을 수 없습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{recentSearchId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "삭제할 최근 검색어 ID", required = true, example = "101")
            @PathVariable @Schema(example = "101") Long recentSearchId
    ) {
        recentSearchCommandService.delete(memberId(jwt), recentSearchId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "검색 타입별 최근 검색어 전체 삭제",
            description = "선택한 CONTENT 또는 PLACE 최근 검색어를 모두 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "최근 검색어 전체 삭제 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 없거나 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping
    public ResponseEntity<Void> clear(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "삭제할 검색 도메인", required = true, example = "PLACE")
            @RequestParam @Schema(example = "PLACE") RecentSearchType type
    ) {
        recentSearchCommandService.clear(memberId(jwt), type);
        return ResponseEntity.noContent().build();
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
