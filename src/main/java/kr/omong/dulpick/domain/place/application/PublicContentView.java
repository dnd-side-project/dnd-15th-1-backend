package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.ContentPublicationStatus;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;

import java.util.List;

public record PublicContentView(
        Long contentId,
        String originalUrl,
        ContentSourceType sourceType,
        String title,
        String content,
        String thumbnailUrl,
        int placeCount,
        ContentPublicationStatus publicationStatus,
        List<MemberPlaceView> places
) {
}
