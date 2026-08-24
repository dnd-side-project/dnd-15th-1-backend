package kr.omong.dulpick.domain.place.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import kr.omong.dulpick.domain.place.application.PlaceImageStorageService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/place-images")
public class PlaceImageController {

    private final PlaceImageStorageService storageService;

    public PlaceImageController(PlaceImageStorageService storageService) {
        this.storageService = storageService;
    }

    @Operation(summary = "장소 이미지 조회", description = "고유 키로 서버에 저장된 카카오 장소 이미지를 반환합니다.")
    @SecurityRequirements
    @GetMapping("/{storageKey}")
    public ResponseEntity<byte[]> findImage(
            @Parameter(description = "장소 이미지 고유 키", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable @Schema(example = "550e8400-e29b-41d4-a716-446655440000") String storageKey
    ) {
        PlaceImageStorageService.StoredImage image = storageService.load(storageKey);
        return ResponseEntity.ok()
                .contentType(image.contentType())
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .body(image.bytes());
    }
}
