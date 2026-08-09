package kr.omong.dulpick.domain.place.application;

public interface PlaceVerifier {

    VerifiedPlace verify(ExtractedPlace extractedPlace);
}
