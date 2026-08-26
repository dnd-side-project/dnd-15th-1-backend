package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.config.ContentImageBackfillProperties;
import kr.omong.dulpick.domain.place.domain.Content;
import kr.omong.dulpick.domain.place.domain.ContentRepository;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.infrastructure.PublicInstagramMetadataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ContentImageBackfillService {

    private static final Logger logger = LoggerFactory.getLogger(ContentImageBackfillService.class);
    private static final List<ContentSourceType> INSTAGRAM_TYPES = List.of(
            ContentSourceType.INSTAGRAM_REEL,
            ContentSourceType.INSTAGRAM_POST
    );

    private final ContentRepository contentRepository;
    private final PublicInstagramMetadataProvider metadataProvider;
    private final ContentImageStorageService imageStorageService;
    private final ContentImageBackfillProperties properties;

    public ContentImageBackfillService(
            ContentRepository contentRepository,
            PublicInstagramMetadataProvider metadataProvider,
            ContentImageStorageService imageStorageService,
            ContentImageBackfillProperties properties
    ) {
        this.contentRepository = contentRepository;
        this.metadataProvider = metadataProvider;
        this.imageStorageService = imageStorageService;
        this.properties = properties;
    }

    public Result backfill() {
        List<Content> contents = contentRepository
                .findAllBySourceTypeInOrderByIdAsc(INSTAGRAM_TYPES)
                .stream()
                .limit(properties.maxContents())
                .toList();
        int succeeded = 0;
        int failed = 0;
        for (int index = 0; index < contents.size(); index++) {
            Content content = contents.get(index);
            if (backfill(content)) {
                succeeded++;
            } else {
                failed++;
            }
            waitBetweenRequests(index, contents.size());
        }
        return new Result(contents.size(), succeeded, failed);
    }

    private boolean backfill(Content content) {
        List<String> imageUrls = new ArrayList<>();
        try {
            metadataProvider.fetchImageUrls(content.getCanonicalUrl())
                    .forEach(url -> addIfPresent(imageUrls, url));
        } catch (RuntimeException exception) {
            logger.warn(
                    "Instagram image metadata backfill failed: contentId={}, cause={}",
                    content.getId(),
                    exception.getClass().getSimpleName()
            );
        }
        if (imageUrls.isEmpty()) {
            addIfPresent(imageUrls, content.getThumbnailUrl());
        }
        if (imageUrls.isEmpty()) {
            logger.warn("Instagram image backfill skipped: contentId={}, no image URL", content.getId());
            return false;
        }
        try {
            imageStorageService.refreshExistingIfAvailable(content, imageUrls);
            return imageStorageService.hasAllStoredImages(content.getId());
        } catch (RuntimeException exception) {
            logger.warn(
                    "Instagram image backfill failed: contentId={}, cause={}",
                    content.getId(),
                    exception.getClass().getSimpleName()
            );
            return false;
        }
    }

    private void addIfPresent(List<String> imageUrls, String imageUrl) {
        if (imageUrl != null && !imageUrl.isBlank() && !imageUrls.contains(imageUrl)) {
            imageUrls.add(imageUrl);
        }
    }

    private void waitBetweenRequests(int index, int total) {
        if (index == total - 1 || properties.delayMillis() == 0) {
            return;
        }
        try {
            Thread.sleep(properties.delayMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Content image backfill interrupted", exception);
        }
    }

    public record Result(
            int total,
            int succeeded,
            int failed
    ) {
    }
}
