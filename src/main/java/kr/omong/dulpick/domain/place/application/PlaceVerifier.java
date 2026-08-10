package kr.omong.dulpick.domain.place.application;

public interface PlaceVerifier {

    PlaceVerificationResult verify(ExtractedPlace extractedPlace);
}
