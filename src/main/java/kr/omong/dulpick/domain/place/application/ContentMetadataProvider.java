package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.ContentSourceType;

public interface ContentMetadataProvider {

    boolean supports(ContentSourceType sourceType);

    ContentMetadata fetch(String canonicalUrl, ContentSourceType sourceType);
}
