package kr.omong.dulpick.domain.place.application;

import java.util.List;

public interface PlaceAnalyzer {

    List<ExtractedPlace> analyze(ContentMetadata metadata);
}
