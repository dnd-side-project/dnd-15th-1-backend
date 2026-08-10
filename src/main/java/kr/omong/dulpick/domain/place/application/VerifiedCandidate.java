package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.PlaceVerificationStatus;

public record VerifiedCandidate(
        ExtractedPlace extracted,
        VerifiedPlace verified,
        PlaceVerificationStatus verificationStatus
) {
}
