package kr.omong.dulpick.domain.place.application;

public record ExtractedPlace(
        String name,
        String addressHint,
        String evidence,
        String mentionType
) {
}
