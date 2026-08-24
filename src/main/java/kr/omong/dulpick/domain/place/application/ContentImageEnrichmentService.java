package kr.omong.dulpick.domain.place.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executor;

@Service
public class ContentImageEnrichmentService {

    private static final Logger logger = LoggerFactory.getLogger(ContentImageEnrichmentService.class);

    private final ContentImageStorageService storageService;
    private final Executor executor;

    public ContentImageEnrichmentService(
            ContentImageStorageService storageService,
            @Qualifier("contentImageExecutor") Executor executor
    ) {
        this.storageService = storageService;
        this.executor = executor;
    }

    public void dispatch(Long contentId, List<String> sourceUrls) {
        if (contentId == null || sourceUrls == null || sourceUrls.isEmpty()) {
            return;
        }
        List<String> imageUrls = List.copyOf(sourceUrls);
        executor.execute(() -> store(contentId, imageUrls));
    }

    private void store(Long contentId, List<String> sourceUrls) {
        try {
            storageService.storeIfAvailable(contentId, sourceUrls);
        } catch (RuntimeException exception) {
            logger.warn(
                    "Content image enrichment failed: contentId={}, cause={}",
                    contentId,
                    exception.getClass().getSimpleName()
            );
        }
    }
}
