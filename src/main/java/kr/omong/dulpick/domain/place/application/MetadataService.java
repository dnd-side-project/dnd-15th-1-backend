package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.application.exception.MetadataUnavailableException;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MetadataService {

    private final List<ContentMetadataProvider> providers;

    public MetadataService(List<ContentMetadataProvider> providers) {
        this.providers = providers;
    }

    public ContentMetadata fetch(String canonicalUrl, ContentSourceType sourceType) {
        MetadataUnavailableException failure = null;
        for (ContentMetadataProvider provider : providers) {
            if (!provider.supports(sourceType)) {
                continue;
            }
            try {
                return provider.fetch(canonicalUrl, sourceType);
            } catch (MetadataUnavailableException exception) {
                failure = exception;
            }
        }
        if (failure != null) {
            throw failure;
        }
        throw new MetadataUnavailableException();
    }
}
