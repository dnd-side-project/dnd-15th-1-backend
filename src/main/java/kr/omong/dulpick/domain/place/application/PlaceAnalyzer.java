package kr.omong.dulpick.domain.place.application;

import java.util.List;

public interface PlaceAnalyzer {

    List<ExtractedPlace> analyze(ContentMetadata metadata);

    default String modelKey() {
        return "default";
    }

    default String promptVersion() {
        return "place-extraction-v3";
    }
}
