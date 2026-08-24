package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.ContentSourceType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PublicContentView(
        Long contentId,
        String canonicalUrl,
        ContentSourceType sourceType,
        ContentAuthorView author,
        LocalDate publishedOn,
        ContentEngagementView engagement,
        String title,
        String caption,
        String thumbnailUrl,
        List<String> imageKeys,
        int placeCount,
        List<PublicPlaceView> places
) {

    public PublicContentView {
        imageKeys = imageKeys == null ? List.of() : List.copyOf(imageKeys);
        places = places == null ? List.of() : List.copyOf(places);
    }

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
