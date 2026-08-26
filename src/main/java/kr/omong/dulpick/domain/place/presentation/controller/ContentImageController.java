package kr.omong.dulpick.domain.place.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import kr.omong.dulpick.domain.place.application.ContentImageStorageService;
import kr.omong.dulpick.global.exception.ErrorResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/content-images")
public class ContentImageController {

    private final ContentImageStorageService imageStorageService;

    public ContentImageController(ContentImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @Operation(
            summary = "게시글 이미지 조회",
            description = "이미지 고유 키로 서버에 저장된 공개 게시글 이미지를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이미지 조회 성공"),
            @ApiResponse(responseCode = "502", description = "이미지를 불러올 수 없음", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirements
    @GetMapping("/{imageKey}")
    public ResponseEntity<byte[]> findImage(
            @Parameter(description = "게시글 이미지 고유 키", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable @Schema(example = "550e8400-e29b-41d4-a716-446655440000") String imageKey
    ) {
        ContentImageStorageService.StoredImage image = imageStorageService.load(imageKey);
        return ResponseEntity.ok()
                .contentType(image.contentType())
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .body(image.bytes());
    }
}
