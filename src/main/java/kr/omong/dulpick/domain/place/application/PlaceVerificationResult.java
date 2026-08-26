package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.PlaceVerificationStatus;

public record PlaceVerificationResult(
        VerifiedPlace place,
        PlaceVerificationStatus status
) {

    public PlaceVerificationResult {
        if (status != PlaceVerificationStatus.VERIFIED
                && status != PlaceVerificationStatus.REVIEW_REQUIRED) {
            throw new IllegalArgumentException("Unsupported place verification status");
        }
    }
}
