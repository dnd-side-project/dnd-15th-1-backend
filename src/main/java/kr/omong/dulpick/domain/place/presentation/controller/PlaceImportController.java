package kr.omong.dulpick.domain.place.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.omong.dulpick.domain.place.application.PlaceCommandService;
import kr.omong.dulpick.domain.place.application.PlaceImportService;
import kr.omong.dulpick.domain.place.presentation.dto.request.PlaceConfirmRequest;
import kr.omong.dulpick.domain.place.presentation.dto.request.PlaceImportRequest;
import kr.omong.dulpick.domain.place.presentation.dto.response.MemberPlaceResponse;
import kr.omong.dulpick.domain.place.presentation.dto.response.PlaceImportResponse;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
            summary = "Instagram 게시물·릴스 장소 분석",
            description = "하나의 엔드포인트에서 게시물과 릴스의 제목·캡션·설명을 분석하고 Kakao 장소 검증 결과를 반환합니다."
    )
    @PostMapping
    public ResponseEntity<PlaceImportResponse> importContent(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PlaceImportRequest request
    ) {
        return ResponseEntity.status(201).body(PlaceImportResponse.from(
                placeImportService.importLink(memberId(jwt), request.sourceUrl())
        ));
    }

    @Operation(
            summary = "장소 분석 결과 조회",
            description = "본인이 요청한 장소 분석 작업의 상태와 Kakao 검증 완료 후보를 조회합니다."
    )
    @GetMapping("/{importId}")
    public ResponseEntity<PlaceImportResponse> get(
            @AuthenticationPrincipal Jwt jwt,
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
    @PostMapping("/{importId}/confirm")
    public ResponseEntity<List<MemberPlaceResponse>> confirm(
            @AuthenticationPrincipal Jwt jwt,
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
        return ResponseEntity.ok(placeCommandService.confirm(
                        memberId(jwt),
                        importId,
                        selections
                ).stream()
                .map(MemberPlaceResponse::from)
                .toList());
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
