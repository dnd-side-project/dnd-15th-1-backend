package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.domain.PlaceImportStatus;

import java.util.List;

public record PlaceImportView(
        Long importId,
        String originalUrl,
        ContentSourceType sourceType,
        String title,
        String content,
        String thumbnailUrl,
        PlaceImportStatus status,
        String failureCode,
        List<PlaceCandidateView> candidates
) {
}
