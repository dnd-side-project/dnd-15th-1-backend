package kr.omong.dulpick.domain.place.application;

public record PlaceCandidateView(
        Long candidateId,
        Long placeId,
        String name,
        String address,
        String roadAddress,
        String kakaoPlaceId,
        String category,
        String evidence,
        String mentionType
) {
}
