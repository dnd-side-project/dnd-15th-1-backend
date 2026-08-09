package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.ContentPublicationStatus;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;

import java.util.List;
import java.time.Instant;
import java.time.LocalDate;

public record PublicContentView(
        Long contentId,
        String originalUrl,
        ContentSourceType sourceType,
        ContentAuthorView author,
        LocalDate publishedOn,
        ContentEngagementView engagement,
        String title,
        String content,
        String thumbnailUrl,
        int placeCount,
        ContentPublicationStatus publicationStatus,
        List<MemberPlaceView> places
) {

    public record ContentAuthorView(
            String displayName,
            String username
    ) {
    }

    public record ContentEngagementView(
            Long likeCount,
            Long commentCount,
            Instant checkedAt
    ) {
    }
}
