package kr.omong.dulpick.domain.place.application;

public record VerifiedCandidate(
        ExtractedPlace extracted,
        VerifiedPlace verified
) {
}
